import { CurrencyPipe } from '@angular/common';
import { Component, ElementRef, HostListener, Injector, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { catchError, filter, of } from 'rxjs';
import { CarritoService } from '../../core/api/carrito.service';
import { Material } from '../../core/api/modelos';
import { CategoriaPublica, TiendaService } from '../../core/api/tienda.service';
import { AuthService } from '../../core/auth/auth.service';
import { CONDICIONES_VENTA } from '../../core/tienda/condiciones-venta';

type Panel = 'joyeria' | 'cuenta' | 'busqueda' | 'movil' | null;

/** Marco superior de la tienda: aviso de envío, navegación con categorías y materiales reales, búsqueda, cuenta y bolsa. */
@Component({
  selector: 'app-cabecera',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './cabecera.html',
  styleUrl: './cabecera.scss',
})
export class Cabecera {
  protected readonly auth = inject(AuthService);
  protected readonly carrito = inject(CarritoService);
  private readonly router = inject(Router);
  private readonly tienda = inject(TiendaService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly injector = inject(Injector);

  protected readonly condiciones = CONDICIONES_VENTA;
  protected readonly categorias = toSignal(this.tienda.categorias().pipe(catchError(() => of([]))), {
    initialValue: [] as CategoriaPublica[],
  });
  protected readonly materiales = toSignal(this.tienda.materiales().pipe(catchError(() => of([]))), {
    initialValue: [] as Material[],
  });
  protected readonly abierto = signal<Panel>(null);

  private readonly campoBusqueda = viewChild<ElementRef<HTMLInputElement>>('campoBusqueda');

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.abierto.set(null));
  }

  protected nombreMaterial(m: Material): string {
    return m.ley ? `${m.tipo} ${m.ley}` : m.tipo;
  }

  protected idMaterial(tipo: string): number | null {
    return this.materiales().find((m) => m.tipo.toLowerCase() === tipo.toLowerCase())?.id ?? null;
  }

  protected alternar(panel: Exclude<Panel, null>): void {
    const siguiente = this.abierto() === panel ? null : panel;
    this.abierto.set(siguiente);
    if (siguiente === 'busqueda') {
      afterNextRender(() => this.campoBusqueda()?.nativeElement.focus(), { injector: this.injector });
    }
  }

  protected buscar(evento: SubmitEvent, campo: HTMLInputElement): void {
    evento.preventDefault();
    const q = campo.value.trim();
    void this.router.navigate(['/catalogo'], { queryParams: q ? { q } : {} });
    campo.value = '';
  }

  protected salir(): void {
    this.abierto.set(null);
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/'));
  }

  @HostListener('document:keydown.escape')
  protected cerrarConEscape(): void {
    if (this.abierto()) this.abierto.set(null);
  }

  @HostListener('document:click', ['$event.target'])
  protected cerrarFuera(destino: EventTarget | null): void {
    const panel = this.abierto();
    if ((panel === 'joyeria' || panel === 'cuenta') && destino instanceof Node && !this.host.nativeElement.contains(destino)) {
      this.abierto.set(null);
    }
  }
}
