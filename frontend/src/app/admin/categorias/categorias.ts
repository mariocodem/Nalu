import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogoAdminService } from '../../core/api/catalogo-admin.service';
import { CategoriaAdmin } from '../../core/api/modelos';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';
import { EditorSeo } from '../seo/editor-seo';

interface FilaCategoria {
  categoria: CategoriaAdmin;
  nivel: number;
}

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };

/** RF03: árbol de categorías; se crean, editan y activan o desactivan (no se borran). */
@Component({
  selector: 'app-admin-categorias',
  imports: [ReactiveFormsModule, EditorSeo],
  template: `
    <section class="pagina">
      <h1>Categorías</h1>
      @if (errores().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }

      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr><th scope="col">Nombre</th><th scope="col">Slug</th><th scope="col">Estado</th><th scope="col"><span class="sr">Acciones</span></th></tr></thead>
          <tbody>
            @for (fila of filas(); track fila.categoria.id) {
              <tr>
                <td [style.padding-left.rem]="0.75 + fila.nivel * 1.25">{{ fila.nivel ? '↳ ' : '' }}{{ fila.categoria.nombre }}</td>
                <td><code>{{ fila.categoria.slug }}</code></td>
                <td><span class="etiqueta-estado" [class.etiqueta-estado--publicado]="fila.categoria.activa">
                  {{ fila.categoria.activa ? 'Activa' : 'Inactiva' }}</span></td>
                <td class="acciones">
                  <button type="button" class="boton boton--secundario boton--pequeno" (click)="editar(fila.categoria)">Editar</button>
                  <button type="button" class="boton boton--secundario boton--pequeno" (click)="alternar(fila.categoria)">
                    {{ fila.categoria.activa ? 'Desactivar' : 'Activar' }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="nota">Aún no hay categorías.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <section class="panel-seccion">
        <h2>{{ editando() ? 'Editar categoría' : 'Nueva categoría' }}</h2>
        <form class="formulario" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
          <div class="formulario__fila">
            <div class="campo">
              <label for="c-nombre">Nombre</label>
              <input id="c-nombre" formControlName="nombre" maxlength="80" [attr.aria-invalid]="!!errores().campos['nombre']" />
              @if (errores().campos['nombre']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="c-slug">Slug (opcional)</label>
              <input id="c-slug" formControlName="slug" maxlength="100" placeholder="Se genera del nombre"
                     [attr.aria-invalid]="!!errores().campos['slug']" />
              @if (errores().campos['slug']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="c-padre">Categoría padre</label>
              <select id="c-padre" formControlName="idPadre">
                <option [ngValue]="null">— Ninguna (raíz) —</option>
                @for (fila of filas(); track fila.categoria.id) {
                  @if (fila.categoria.id !== editando()?.id) {
                    <option [ngValue]="fila.categoria.id">{{ '— '.repeat(fila.nivel) }}{{ fila.categoria.nombre }}</option>
                  }
                }
              </select>
            </div>
          </div>
          <div class="campo">
            <label for="c-descripcion">Descripción (opcional)</label>
            <textarea id="c-descripcion" formControlName="descripcion" maxlength="2000"></textarea>
          </div>
          <div class="acciones">
            <button class="boton boton--primario" type="submit" [disabled]="formulario.invalid || guardando()">
              {{ editando() ? 'Guardar cambios' : 'Crear categoría' }}</button>
            @if (editando()) {
              <button type="button" class="boton boton--secundario" (click)="cancelar()">Cancelar</button>
            }
          </div>
        </form>
      </section>
      @if (editando(); as c) {
        <section class="panel-seccion">
          <h2>SEO de «{{ c.nombre }}»</h2>
          <app-editor-seo tipo="categorias" [idRecurso]="c.id" />
        </section>
      }
    </section>
  `,
})
export class Categorias implements OnInit {
  private readonly catalogo = inject(CatalogoAdminService);
  protected readonly arbol = signal<CategoriaAdmin[]>([]);
  protected readonly editando = signal<CategoriaAdmin | null>(null);
  protected readonly errores = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly guardando = signal(false);
  protected readonly filas = computed(() => aplanar(this.arbol(), 0));

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    slug: [''],
    descripcion: [''],
    idPadre: [null as number | null],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected editar(c: CategoriaAdmin): void {
    this.editando.set(c);
    this.errores.set(SIN_ERRORES);
    this.formulario.setValue({ nombre: c.nombre, slug: c.slug, descripcion: c.descripcion ?? '', idPadre: c.idPadre });
  }

  protected cancelar(): void {
    this.editando.set(null);
    this.formulario.reset();
  }

  protected guardar(): void {
    this.errores.set(SIN_ERRORES);
    this.guardando.set(true);
    const v = this.formulario.getRawValue();
    const datos = { nombre: v.nombre, slug: v.slug || null, descripcion: v.descripcion || null, idPadre: v.idPadre };
    const actual = this.editando();
    const peticion = actual ? this.catalogo.actualizarCategoria(actual.id, datos) : this.catalogo.crearCategoria(datos);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cancelar();
        this.cargar();
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errores.set(erroresDe(e));
      },
    });
  }

  protected alternar(c: CategoriaAdmin): void {
    this.catalogo.cambiarEstadoCategoria(c.id, !c.activa).subscribe({
      next: () => this.cargar(),
      error: (e: unknown) => this.errores.set(erroresDe(e)),
    });
  }

  private cargar(): void {
    this.catalogo.categorias().subscribe({
      next: (a) => this.arbol.set(a),
      error: (e: unknown) => this.errores.set(erroresDe(e)),
    });
  }
}

export function aplanar(categorias: CategoriaAdmin[], nivel: number): FilaCategoria[] {
  return categorias.flatMap((categoria) => [{ categoria, nivel }, ...aplanar(categoria.hijos, nivel + 1)]);
}
