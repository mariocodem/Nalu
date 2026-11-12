import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, RESPONSE_INIT, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';
import { FichaProducto, TiendaService, VarianteFicha } from '../../core/api/tienda.service';
import { CarritoService } from '../../core/api/carrito.service';
import { erroresDe } from '../../core/api/problema';
import { SeoService } from '../../core/seo/seo.service';

const ROLES = { METAL_PRINCIPAL: 'Metal', PIEDRA: 'Piedra', SECUNDARIO: 'Detalle' } as const;

/** RF07: ficha de la pieza en su vitrina, con la cédula como panel de compra. */
@Component({
  selector: 'app-ficha-producto',
  imports: [RouterLink, CurrencyPipe, DecimalPipe],
  templateUrl: './ficha-producto.html',
  styleUrl: './ficha-producto.scss',
})
export class FichaProductoPagina {
  readonly slug = input.required<string>();

  private readonly tienda = inject(TiendaService);
  private readonly seo = inject(SeoService);
  private readonly respuesta = inject(RESPONSE_INIT, { optional: true });
  private readonly carrito = inject(CarritoService);
  protected readonly agregando = signal(false);
  protected readonly mensaje = signal<{ texto: string; error: boolean } | null>(null);

  protected readonly roles = ROLES;
  protected readonly noEncontrada = signal(false);
  protected readonly imagenActiva = signal(0);
  protected readonly varianteElegida = signal<VarianteFicha | null>(null);

  protected readonly ficha = toSignal(
    toObservable(this.slug).pipe(
      switchMap((slug) => this.tienda.ficha(slug).pipe(catchError(() => of(null)))),
      tap((f) => this.mostrar(f)),
    ),
    { initialValue: null },
  );

  /** Si hay una sola variante, queda elegida; si hay varias, hay que elegir talla. */
  protected readonly variante = computed(() => {
    const f = this.ficha();
    if (!f) return null;
    return this.varianteElegida() ?? (f.variantes.length === 1 ? f.variantes[0] : null);
  });

  protected readonly precio = computed(() => this.variante()?.precio ?? this.ficha()?.precioDesde ?? null);
  protected readonly variosPrecios = computed(() => new Set(this.ficha()?.variantes.map((v) => v.precio)).size > 1);

  protected estrellas(n: number): string {
    const llenas = Math.round(n);
    return '★'.repeat(llenas) + '☆'.repeat(5 - llenas);
  }

  protected elegir(v: VarianteFicha): void {
    if (v.disponible) {
      this.varianteElegida.set(v);
      this.mensaje.set(null);
    }
  }

  protected agregar(): void {
    const v = this.variante();
    if (!v) {
      this.mensaje.set({ texto: 'Elige una talla', error: true });
      return;
    }
    this.agregando.set(true);
    this.carrito.agregar(v.id).subscribe({
      next: () => {
        this.agregando.set(false);
        this.mensaje.set({ texto: 'Añadida a tu bolsa.', error: false });
      },
      error: (e: unknown) => {
        this.agregando.set(false);
        this.mensaje.set({ texto: erroresDe(e).general ?? 'No se pudo añadir', error: true });
      },
    });
  }

  private mostrar(f: FichaProducto | null): void {
    this.imagenActiva.set(0);
    this.varianteElegida.set(null);
    this.mensaje.set(null);
    this.noEncontrada.set(!f);
    if (!f) {
      if (this.respuesta) this.respuesta.status = 404;
      this.seo.aplicar({ titulo: 'Pieza no encontrada · Nalu', descripcion: 'Esta pieza no existe o ya no está disponible.' });
      return;
    }
    this.seo.aplicar({
      titulo: f.seo.titulo,
      descripcion: f.seo.descripcion,
      palabrasClave: f.seo.palabrasClave,
      urlCanonica: f.seo.urlCanonica,
      imagen: (f.seo.jsonLd['image'] as string[] | undefined)?.[0] ?? null,
      jsonLd: f.seo.jsonLd,
    });
  }
}
