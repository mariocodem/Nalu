import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { Material, Pagina } from '../../core/api/modelos';
import { CategoriaPublica, ProductoTarjeta, TiendaService } from '../../core/api/tienda.service';
import { SeoService } from '../../core/seo/seo.service';
import { CONDICIONES_VENTA } from '../../core/tienda/condiciones-venta';
import { VitrinaPieza } from '../../shared/vitrina/vitrina-pieza';

type Estado = 'cargando' | 'listo' | 'error';
interface SalaEsmeralda { id: number; total: number; piezas: ProductoTarjeta[] }

/** Fotografía editorial por categoría (frontend/public/editorial). Una categoría sin foto se muestra solo con su nombre. */
const FOTO_CATEGORIA: Record<string, { src: string; alt: string }> = {
  anillos: { src: '/editorial/categoria-anillos.jpg', alt: 'Mano con anillos dorados finos combinados en dos dedos' },
  aretes: { src: '/editorial/categoria-aretes.jpg', alt: 'Argollas trenzadas doradas sobre una piedra blanca al sol' },
  collares: { src: '/editorial/categoria-collares.jpg', alt: 'Dos cadenas doradas superpuestas con un dije en forma de gota' },
  pulseras: { src: '/editorial/categoria-pulseras.jpg', alt: 'Pulsera de eslabones dorados sobre una revista abierta' },
};

/** Definición de cada metal por su ley. Solo datos de definición, sin promesas comerciales. */
const DEFINICION_METAL: Record<string, string> = {
  'acero 316l': 'Acero inoxidable de grado quirúrgico.',
  'plata 925': 'Plata de ley: 92,5 % de plata pura.',
  'oro 18k': 'Oro de 18 quilates: 75 % de oro puro.',
};

const NOVEDADES = 8;
const SALA_ESMERALDA = 3;

/** Inicio de la tienda: portada editorial, categorías, sala Esmeralda, novedades, metales y condiciones de compra. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, CurrencyPipe, VitrinaPieza],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio implements OnInit {
  private readonly tienda = inject(TiendaService);
  private readonly seo = inject(SeoService);

  protected readonly condiciones = CONDICIONES_VENTA;
  protected readonly estado = signal<Estado>('cargando');
  protected readonly novedades = signal<ProductoTarjeta[]>([]);
  protected readonly esmeralda = signal<SalaEsmeralda | null>(null);
  protected readonly materiales = signal<Material[]>([]);
  protected readonly categorias = signal<CategoriaPublica[]>([]);
  protected readonly metales = computed(() => this.materiales().filter((m) => m.ley !== null));
  protected readonly esqueleto = Array.from({ length: 4 }, (_, i) => i);

  ngOnInit(): void {
    this.seo.aplicar({
      titulo: 'Nalu · Joyería colombiana en acero, plata y oro',
      descripcion: 'Joyería colombiana: esmeraldas y cuarzos en acero, plata u oro. Compra en línea con envío a toda Colombia.',
    });
    this.tienda.categorias().pipe(catchError(() => of([]))).subscribe((c) => this.categorias.set(c));
    this.cargarPiezas();
  }

  protected cargarPiezas(): void {
    this.estado.set('cargando');
    forkJoin({
      materiales: this.tienda.materiales(),
      recientes: this.tienda.productos({ sort: 'fechaCreacion,desc', size: NOVEDADES + SALA_ESMERALDA }),
    })
      .pipe(
        switchMap(({ materiales, recientes }) => {
          const id = materiales.find((m) => m.tipo.toLowerCase() === 'esmeralda')?.id ?? null;
          const sala: Observable<SalaEsmeralda | null> = id === null
            ? of(null)
            : this.tienda.productos({ material: id, sort: 'fechaCreacion,desc', size: SALA_ESMERALDA }).pipe(
                map((p: Pagina<ProductoTarjeta>) => ({ id, total: p.totalElementos, piezas: p.contenido })),
              );
          return sala.pipe(map((esmeralda) => ({ materiales, recientes, esmeralda })));
        }),
      )
      .subscribe({
        next: ({ materiales, recientes, esmeralda }) => {
          const enSala = new Set(esmeralda?.piezas.map((p) => p.slug));
          this.materiales.set(materiales);
          this.esmeralda.set(esmeralda?.piezas.length ? esmeralda : null);
          const lista = recientes.contenido.filter((p) => !enSala.has(p.slug)).slice(0, NOVEDADES);
          // filas completas en las rejillas de 4 y 2 columnas; «Ver todas» lleva al resto
          this.novedades.set(lista.length >= 4 ? lista.slice(0, lista.length - (lista.length % 4)) : lista);
          this.estado.set('listo');
        },
        error: () => this.estado.set('error'),
      });
  }

  protected fotoCategoria(slug: string) {
    return FOTO_CATEGORIA[slug] ?? null;
  }

  protected nombreMaterial(m: Material): string {
    return m.ley ? `${m.tipo} ${m.ley}` : m.tipo;
  }

  protected definicionMetal(m: Material): string | null {
    return DEFINICION_METAL[this.nombreMaterial(m).toLowerCase()] ?? null;
  }
}
