import { Component, RESPONSE_INIT, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, map, of, switchMap, tap } from 'rxjs';
import { Material, Pagina } from '../../core/api/modelos';
import { CategoriaDetalle, CategoriaPublica, ProductoTarjeta, TiendaService } from '../../core/api/tienda.service';
import { SeoService } from '../../core/seo/seo.service';
import { VitrinaPieza } from '../../shared/vitrina/vitrina-pieza';

const TAMANO = 24;

/**
 * RF06: catálogo con búsqueda y filtros. Los filtros viven en la URL (?q&material&precioMin…),
 * así cada combinación es un enlace que se puede compartir y que el servidor renderiza.
 */
@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, RouterLink, VitrinaPieza],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.scss',
})
export class Catalogo {
  /** Slug de la categoría (ruta /catalogo/:categoria); vacío en /catalogo. */
  readonly categoria = input<string>();

  private readonly tienda = inject(TiendaService);
  private readonly seo = inject(SeoService);
  private readonly router = inject(Router);
  private readonly respuesta = inject(RESPONSE_INIT, { optional: true });
  private readonly parametros = toSignal(inject(ActivatedRoute).queryParamMap, { requireSync: true });

  protected readonly materiales = toSignal(this.tienda.materiales(), { initialValue: [] as Material[] });
  protected readonly categorias = toSignal(this.tienda.categorias(), { initialValue: [] as CategoriaPublica[] });
  protected readonly detalle = signal<CategoriaDetalle | null>(null);
  protected readonly noEncontrada = signal(false);
  protected readonly cargando = signal(true);

  // Copia editable de los filtros para el formulario.
  protected q = '';
  protected material = '';
  protected precioMin: number | null = null;
  protected precioMax: number | null = null;
  protected disponible = false;
  protected orden = '';

  protected readonly pagina = toSignal(
    combineLatest([toObservable(this.categoria), inject(ActivatedRoute).queryParamMap]).pipe(
      tap(() => this.cargando.set(true)),
      switchMap(([slug, p]) => {
        this.leerFiltros();
        const detalle$ = slug ? this.tienda.categoria(slug).pipe(catchError(() => of(null))) : of(null);
        return detalle$.pipe(
          tap((d) => this.mostrarCategoria(slug, d)),
          switchMap((d) =>
            slug && !d
              ? of(null)
              : this.tienda.productos({
                  categoria: slug || null,
                  q: p.get('q'),
                  material: num(p.get('material')),
                  precioMin: num(p.get('precioMin')),
                  precioMax: num(p.get('precioMax')),
                  disponible: p.get('disponible') === 'true',
                  sort: p.get('sort'),
                  page: num(p.get('pagina')) ?? 0,
                  size: TAMANO,
                }),
          ),
          catchError(() => of(null)),
        );
      }),
      tap(() => this.cargando.set(false)),
      map((r) => r as Pagina<ProductoTarjeta> | null),
    ),
    { initialValue: null },
  );

  protected nombreMaterial(m: Material): string {
    return m.ley ? `${m.tipo} ${m.ley}` : m.tipo;
  }

  protected aplicar(): void {
    void this.router.navigate([], {
      queryParams: {
        q: this.q.trim() || null,
        material: this.material || null,
        precioMin: this.precioMin || null,
        precioMax: this.precioMax || null,
        disponible: this.disponible || null,
        sort: this.orden || null,
        pagina: null,
      },
    });
  }

  protected limpiar(): void {
    void this.router.navigate([], { queryParams: {} });
  }

  protected irA(pagina: number): void {
    void this.router.navigate([], { queryParams: { pagina: pagina || null }, queryParamsHandling: 'merge' });
  }

  private leerFiltros(): void {
    const p = this.parametros();
    this.q = p.get('q') ?? '';
    this.material = p.get('material') ?? '';
    this.precioMin = num(p.get('precioMin'));
    this.precioMax = num(p.get('precioMax'));
    this.disponible = p.get('disponible') === 'true';
    this.orden = p.get('sort') ?? '';
  }

  private mostrarCategoria(slug: string | undefined, detalle: CategoriaDetalle | null): void {
    this.detalle.set(detalle);
    this.noEncontrada.set(!!slug && !detalle);
    if (slug && !detalle) {
      if (this.respuesta) this.respuesta.status = 404;
      this.seo.aplicar({ titulo: 'Colección no encontrada · Nalu', descripcion: 'Esta colección no existe.' });
    } else if (detalle) {
      const s = detalle.seo;
      this.seo.aplicar({ titulo: s.titulo, descripcion: s.descripcion, palabrasClave: s.palabrasClave,
        urlCanonica: s.urlCanonica, jsonLd: s.jsonLd });
    } else {
      this.seo.aplicar({
        titulo: 'Catálogo · Nalu',
        descripcion: 'Todas las piezas de Nalu: anillos, collares, aretes y pulseras en acero, plata y oro.',
      });
    }
  }
}

function num(valor: string | null): number | null {
  const n = valor === null || valor === '' ? NaN : Number(valor);
  return Number.isFinite(n) ? n : null;
}
