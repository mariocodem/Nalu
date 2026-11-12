import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component, ElementRef, HostListener, Injector, PLATFORM_ID, afterNextRender, computed, effect, inject, signal, viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { AsistenteService, PiezaSugerida, RolMensaje } from '../../core/api/asistente.service';
import { SIN_CONEXION, erroresDe } from '../../core/api/problema';
import { AuthService } from '../../core/auth/auth.service';
import { Bloque, formatear } from './formato';
import { TarjetaAsistente } from './tarjeta-asistente';

interface MensajeChat {
  id: number; rol: RolMensaje; texto: string; bloques: Bloque[]; piezas: PiezaSugerida[]; fallido?: boolean;
}

const CLAVE_SESION = 'nalu.asistente.v1';
const MAX_HISTORIAL = 20;
const MAX_TEXTO_HISTORIAL = 3500;
export const MAX_MENSAJE = 1000;
const SUGERENCIAS = [
  'Muéstrame piezas con esmeralda',
  '¿Qué diferencia hay entre plata 925 y oro 18K?',
  '¿Cuánto cuesta el envío?',
];

/**
 * Asesora de compras con IA (ADR 005): botón flotante y panel de conversación. Solo aparece si el
 * backend tiene el asistente configurado y fuera del panel administrativo. La conversación vive en
 * esta pestaña (sessionStorage) y nunca se guarda en el servidor.
 */
@Component({
  selector: 'app-asistente',
  imports: [TarjetaAsistente],
  templateUrl: './asistente.html',
  styleUrl: './asistente.scss',
})
export class Asistente {
  private readonly api = inject(AsistenteService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly navegador = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly lanzador = viewChild<ElementRef<HTMLButtonElement>>('lanzador');
  private readonly campo = viewChild<ElementRef<HTMLTextAreaElement>>('campo');
  private readonly lista = viewChild<ElementRef<HTMLElement>>('lista');

  protected readonly maxMensaje = MAX_MENSAJE;
  protected readonly disponible = signal(false);
  protected readonly abierto = signal(false);
  protected readonly mensajes = signal<MensajeChat[]>([]);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly borrador = signal('');
  private siguienteId = 1;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  protected readonly visible = computed(() => this.disponible() && !this.url().startsWith('/admin'));
  protected readonly sugerencias = computed(() =>
    this.auth.usuario()?.roles.includes('CLIENTE') ? [...SUGERENCIAS, '¿Cómo va mi pedido?'] : SUGERENCIAS,
  );

  constructor() {
    afterNextRender(() => {
      this.restaurar();
      this.api.estado().subscribe({
        next: (e) => this.disponible.set(e.disponible),
        error: () => this.disponible.set(false),
      });
    });
    effect(() => {
      const mensajes = this.mensajes().filter((m) => !m.fallido);
      if (!this.navegador) return;
      try {
        sessionStorage.setItem(CLAVE_SESION, JSON.stringify(mensajes.map(({ rol, texto, piezas }) => ({ rol, texto, piezas }))));
      } catch {
        // almacenamiento bloqueado o lleno: la conversación sigue en memoria
      }
    });
  }

  protected abrir(): void {
    this.abierto.set(true);
    this.alRenderizar(() => {
      this.campo()?.nativeElement.focus();
      this.bajar();
    });
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.alRenderizar(() => this.lanzador()?.nativeElement.focus());
  }

  @HostListener('document:keydown.escape')
  protected alPulsarEscape(): void {
    if (this.abierto()) this.cerrar();
  }

  /** Las sugerencias desaparecen al usarlas: el foco pasa al campo para no perderse. */
  protected usarSugerencia(texto: string): void {
    this.enviar(texto);
    this.alRenderizar(() => this.campo()?.nativeElement.focus());
  }

  protected reiniciar(): void {
    this.mensajes.set([]);
    this.error.set(null);
    this.campo()?.nativeElement.focus();
  }

  protected alPulsarEnter(evento: Event): void {
    const teclado = evento as KeyboardEvent;
    if (!teclado.shiftKey && !teclado.isComposing) {
      teclado.preventDefault();
      this.enviar();
    }
  }

  protected enviar(texto?: string): void {
    const mensaje = (texto ?? this.borrador()).trim().slice(0, MAX_MENSAJE);
    if (!mensaje || this.enviando()) return;
    const historial = this.mensajes()
      .filter((m) => !m.fallido)
      .slice(-MAX_HISTORIAL)
      .map((m) => ({ rol: m.rol, texto: m.texto.slice(0, MAX_TEXTO_HISTORIAL) }));
    const propio = this.agregar('cliente', mensaje, []);
    if (texto === undefined) this.borrador.set('');
    this.error.set(null);
    this.enviando.set(true);
    this.api.enviar(historial, mensaje).subscribe({
      next: (r) => {
        this.enviando.set(false);
        this.agregar('asistente', r.texto, r.piezas);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.mensajes.update((ms) => ms.map((m) => (m.id === propio ? { ...m, fallido: true } : m)));
        this.error.set(this.mensajeDeError(e));
        this.bajar();
      },
    });
  }

  protected reintentar(): void {
    const fallido = this.mensajes().find((m) => m.fallido);
    if (!fallido) return;
    this.mensajes.update((ms) => ms.filter((m) => m.id !== fallido.id));
    this.enviar(fallido.texto);
  }

  private agregar(rol: RolMensaje, texto: string, piezas: PiezaSugerida[]): number {
    const id = this.siguienteId++;
    this.mensajes.update((ms) => [...ms, { id, rol, texto, bloques: formatear(texto), piezas }]);
    this.bajar();
    return id;
  }

  private mensajeDeError(e: unknown): string {
    if (e instanceof HttpErrorResponse && e.status === 0) return SIN_CONEXION;
    return erroresDe(e).general ?? 'No pude responder. Inténtalo de nuevo.';
  }

  private bajar(): void {
    this.alRenderizar(() => {
      const lista = this.lista()?.nativeElement;
      if (lista) lista.scrollTop = lista.scrollHeight;
    });
  }

  private alRenderizar(accion: () => void): void {
    afterNextRender(accion, { injector: this.injector });
  }

  private restaurar(): void {
    try {
      const guardado = JSON.parse(sessionStorage.getItem(CLAVE_SESION) ?? '[]') as Omit<MensajeChat, 'id' | 'bloques'>[];
      if (!Array.isArray(guardado)) return;
      this.mensajes.set(
        guardado
          .filter((m) => (m.rol === 'cliente' || m.rol === 'asistente') && typeof m.texto === 'string')
          .map((m) => ({ id: this.siguienteId++, rol: m.rol, texto: m.texto, bloques: formatear(m.texto), piezas: m.piezas ?? [] })),
      );
    } catch {
      // sin almacenamiento o datos corruptos: conversación nueva
    }
  }
}
