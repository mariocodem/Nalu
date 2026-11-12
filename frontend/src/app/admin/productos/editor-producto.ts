import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogoAdminService } from '../../core/api/catalogo-admin.service';
import {
  CategoriaAdmin,
  EstadoProducto,
  Imagen,
  Material,
  NOMBRES_ESTADO,
  NOMBRES_ROL,
  ProductoAdmin,
  RolMaterial,
  Variante,
  nombreMaterial,
} from '../../core/api/modelos';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';
import { aplanar } from '../categorias/categorias';
import { EditorSeo } from '../seo/editor-seo';

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };
const SKU = /^[A-Za-z0-9-]{3,40}$/;
const FORMATOS = ['image/jpeg', 'image/png', 'image/webp'];
const MAXIMO_IMAGEN = 5 * 1024 * 1024;

/**
 * CU-11: datos del producto, materiales, variantes, imágenes y publicación. El id llega por la
 * ruta (`withComponentInputBinding`); sin id es un producto nuevo.
 */
@Component({
  selector: 'app-editor-producto',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, EditorSeo],
  templateUrl: './editor-producto.html',
  styleUrl: './editor-producto.scss',
})
export class EditorProducto implements OnInit {
  readonly id = input<string>();

  private readonly catalogo = inject(CatalogoAdminService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly producto = signal<ProductoAdmin | null>(null);
  protected readonly categorias = signal<CategoriaAdmin[]>([]);
  protected readonly materiales = signal<Material[]>([]);
  protected readonly filasCategoria = computed(() => aplanar(this.categorias(), 0));
  protected readonly nombresEstado = NOMBRES_ESTADO;
  protected readonly nombresRol = NOMBRES_ROL;
  protected readonly roles: RolMaterial[] = ['METAL_PRINCIPAL', 'PIEDRA', 'SECUNDARIO'];
  protected readonly nombreMaterial = nombreMaterial;

  protected readonly errores = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly erroresMateriales = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly erroresVariante = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly erroresImagen = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly erroresEstado = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly ocupado = signal(false);

  protected readonly datos = this.fb.group({
    sku: ['', [Validators.required, Validators.pattern(SKU)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    slug: [''],
    descripcion: [''],
    idCategoria: [null as number | null, Validators.required],
    precioBase: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  protected readonly filasMaterial = this.fb.array<ReturnType<EditorProducto['filaMaterial']>>([]);

  protected readonly nuevaVariante = this.fb.group({
    sku: ['', [Validators.required, Validators.pattern(SKU)]],
    talla: [''],
    pesoGramos: [null as number | null, Validators.min(0.01)],
    precioAdicional: [0, [Validators.required, Validators.min(0)]],
    existenciaInicial: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly nuevaImagen = this.fb.group({ textoAlternativo: ['', [Validators.required, Validators.maxLength(160)]] });
  protected archivo: File | null = null;

  ngOnInit(): void {
    this.catalogo.categorias().subscribe((c) => this.categorias.set(c));
    this.catalogo.materiales().subscribe((m) => this.materiales.set(m));
    const id = Number(this.id());
    if (id) {
      this.catalogo.producto(id).subscribe({
        next: (p) => this.mostrar(p),
        error: (e: unknown) => this.errores.set(erroresDe(e)),
      });
    }
  }

  protected filaMaterial(idMaterial: number | null = null, rol: RolMaterial = 'METAL_PRINCIPAL',
                         pesoGramos: number | null = null, quilates: number | null = null) {
    return this.fb.group({
      idMaterial: [idMaterial, Validators.required],
      rol: [rol, Validators.required],
      pesoGramos: [pesoGramos, Validators.min(0.01)],
      quilates: [quilates, Validators.min(0.01)],
    });
  }

  protected guardarDatos(): void {
    this.errores.set(SIN_ERRORES);
    this.aviso.set(null);
    const v = this.datos.getRawValue();
    const solicitud = {
      sku: v.sku.trim().toUpperCase(),
      nombre: v.nombre,
      slug: v.slug || null,
      descripcion: v.descripcion || null,
      idCategoria: v.idCategoria as number,
      precioBase: v.precioBase as number,
    };
    const actual = this.producto();
    this.ocupado.set(true);
    (actual ? this.catalogo.actualizarProducto(actual.id, solicitud) : this.catalogo.crearProducto(solicitud)).subscribe({
      next: (p) => {
        this.ocupado.set(false);
        if (!actual) {
          void this.router.navigate(['/admin/productos', p.id], { replaceUrl: true });
        }
        this.mostrar(p, 'Datos guardados.');
      },
      error: (e: unknown) => this.fallo(e, this.errores),
    });
  }

  protected agregarMaterial(): void {
    this.filasMaterial.push(this.filaMaterial());
  }

  protected quitarMaterial(indice: number): void {
    this.filasMaterial.removeAt(indice);
  }

  protected guardarMateriales(): void {
    const p = this.producto();
    if (!p) return;
    this.erroresMateriales.set(SIN_ERRORES);
    const datos = this.filasMaterial.getRawValue().map((f) => ({ ...f, idMaterial: f.idMaterial as number }));
    this.ocupado.set(true);
    this.catalogo.reemplazarMateriales(p.id, datos).subscribe({
      next: (r) => this.mostrar(r, 'Materiales guardados.'),
      error: (e: unknown) => this.fallo(e, this.erroresMateriales),
    });
  }

  protected crearVariante(): void {
    const p = this.producto();
    if (!p) return;
    this.erroresVariante.set(SIN_ERRORES);
    const v = this.nuevaVariante.getRawValue();
    this.ocupado.set(true);
    this.catalogo
      .agregarVariante(p.id, { ...v, sku: v.sku.trim().toUpperCase(), talla: v.talla || null })
      .subscribe({
        next: (r) => {
          this.nuevaVariante.reset({ sku: '', talla: '', pesoGramos: null, precioAdicional: 0, existenciaInicial: 0 });
          this.mostrar(r, 'Variante creada.');
        },
        error: (e: unknown) => this.fallo(e, this.erroresVariante),
      });
  }

  protected alternarVariante(variante: Variante): void {
    const p = this.producto();
    if (!p) return;
    this.erroresVariante.set(SIN_ERRORES);
    this.catalogo
      .actualizarVariante(p.id, variante.id, {
        talla: variante.talla,
        pesoGramos: variante.pesoGramos,
        precioAdicional: variante.precioAdicional,
        activa: !variante.activa,
      })
      .subscribe({ next: (r) => this.mostrar(r), error: (e: unknown) => this.fallo(e, this.erroresVariante) });
  }

  protected elegirArchivo(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0] ?? null;
    this.erroresImagen.set(SIN_ERRORES);
    if (archivo && (!FORMATOS.includes(archivo.type) || archivo.size > MAXIMO_IMAGEN)) {
      this.erroresImagen.set({ general: 'La imagen debe ser JPEG, PNG o WebP y pesar como máximo 5 MB.', campos: {} });
      this.archivo = null;
      return;
    }
    this.archivo = archivo;
  }

  protected subirImagen(): void {
    const p = this.producto();
    if (!p || !this.archivo) return;
    this.erroresImagen.set(SIN_ERRORES);
    this.ocupado.set(true);
    this.catalogo.subirImagen(p.id, this.archivo, this.nuevaImagen.getRawValue().textoAlternativo).subscribe({
      next: (r) => {
        this.archivo = null;
        this.nuevaImagen.reset();
        this.mostrar(r, 'Imagen subida.');
      },
      error: (e: unknown) => this.fallo(e, this.erroresImagen),
    });
  }

  protected hacerPrincipal(imagen: Imagen): void {
    const p = this.producto();
    if (!p) return;
    this.catalogo
      .actualizarImagen(p.id, imagen.id, { textoAlternativo: imagen.textoAlternativo, orden: imagen.orden, principal: true })
      .subscribe({ next: (r) => this.mostrar(r), error: (e: unknown) => this.fallo(e, this.erroresImagen) });
  }

  protected eliminarImagen(imagen: Imagen): void {
    const p = this.producto();
    if (!p || !confirm('¿Eliminar esta imagen?')) return;
    this.catalogo.eliminarImagen(p.id, imagen.id).subscribe({
      next: (r) => this.mostrar(r, 'Imagen eliminada.'),
      error: (e: unknown) => this.fallo(e, this.erroresImagen),
    });
  }

  protected cambiarEstado(estado: EstadoProducto): void {
    const p = this.producto();
    if (!p) return;
    this.erroresEstado.set(null);
    this.ocupado.set(true);
    this.catalogo.cambiarEstado(p.id, estado).subscribe({
      next: (r) => this.mostrar(r, `Estado: ${NOMBRES_ESTADO[r.estado]}.`),
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erroresEstado.set(erroresDe(e).general);
      },
    });
  }

  private mostrar(p: ProductoAdmin, aviso: string | null = null): void {
    this.ocupado.set(false);
    this.producto.set(p);
    this.aviso.set(aviso);
    this.datos.setValue({
      sku: p.sku,
      nombre: p.nombre,
      slug: p.slug,
      descripcion: p.descripcion ?? '',
      idCategoria: p.idCategoria,
      precioBase: p.precioBase,
    });
    this.filasMaterial.clear();
    for (const m of p.materiales) {
      this.filasMaterial.push(this.filaMaterial(m.idMaterial, m.rol, m.pesoGramos, m.quilates));
    }
  }

  private fallo(error: unknown, destino: ReturnType<typeof signal<ErroresFormulario>>): void {
    this.ocupado.set(false);
    destino.set(erroresDe(error));
  }

  protected get filas(): FormArray {
    return this.filasMaterial;
  }
}
