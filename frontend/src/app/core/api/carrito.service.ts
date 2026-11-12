import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, effect, inject, signal, untracked } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, tap } from 'rxjs';
import { AuthService } from '../auth/auth.service';

// Contrato: docs/arquitectura/api-fase3.md §1
export interface ItemCarrito {
  id: number; idVariante: number; slug: string; nombre: string; talla: string | null;
  imagen: string | null; textoAlternativo: string | null; precioUnitario: number; cantidad: number;
  subtotal: number; maximo: number; disponible: boolean;
}
export interface Carrito { token: string | null; items: ItemCarrito[]; cantidadTotal: number; subtotal: number }

const CLAVE_TOKEN = 'nalu_bolsa';
const VACIO: Carrito = { token: null, items: [], cantidadTotal: 0, subtotal: 0 };

/**
 * La bolsa (RF08). Un visitante guarda en localStorage el identificador de su carrito (no es un
 * dato sensible); al iniciar sesión como cliente, ese carrito pasa a su cuenta.
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly navegador = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly _carrito = signal<Carrito>(VACIO);
  readonly carrito = this._carrito.asReadonly();
  readonly cantidad = computed(() => this._carrito().cantidadTotal);

  constructor() {
    if (!this.navegador) return;
    // Cada vez que cambia la sesión: el cliente recibe su carrito (fusionado); el visitante, el suyo.
    effect(() => {
      const usuario = this.auth.usuario();
      untracked(() => {
        if (usuario?.roles.includes('CLIENTE')) {
          this.fusionar().subscribe();
        } else {
          this.recargar().subscribe();
        }
      });
    });
  }

  recargar(): Observable<Carrito> {
    return this.http.get<Carrito>('/api/v1/carrito', { headers: this.cabeceras() }).pipe(tap((c) => this.guardar(c)));
  }

  agregar(idVariante: number, cantidad = 1): Observable<Carrito> {
    return this.http
      .post<Carrito>('/api/v1/carrito/items', { idVariante, cantidad }, { headers: this.cabeceras() })
      .pipe(tap((c) => this.guardar(c)));
  }

  cambiarCantidad(idItem: number, cantidad: number): Observable<Carrito> {
    return this.http
      .patch<Carrito>(`/api/v1/carrito/items/${idItem}`, { cantidad }, { headers: this.cabeceras() })
      .pipe(tap((c) => this.guardar(c)));
  }

  quitar(idItem: number): Observable<Carrito> {
    return this.http
      .delete<Carrito>(`/api/v1/carrito/items/${idItem}`, { headers: this.cabeceras() })
      .pipe(tap((c) => this.guardar(c)));
  }

  /** Tras crear el pedido, el carrito del servidor ya está convertido. */
  vaciarLocal(): void {
    this._carrito.set(VACIO);
  }

  private fusionar(): Observable<Carrito> {
    return this.http.post<Carrito>('/api/v1/carrito/fusionar', null, { headers: this.cabeceras() }).pipe(
      tap((c) => {
        this.guardarToken(null);
        this.guardar(c);
      }),
    );
  }

  private guardar(c: Carrito): void {
    this._carrito.set(c);
    if (c.token) this.guardarToken(c.token);
  }

  private cabeceras(): HttpHeaders {
    const token = this.leerToken();
    return token ? new HttpHeaders({ 'X-Cart-Token': token }) : new HttpHeaders();
  }

  private leerToken(): string | null {
    try {
      return this.navegador ? localStorage.getItem(CLAVE_TOKEN) : null;
    } catch {
      return null;
    }
  }

  private guardarToken(token: string | null): void {
    try {
      if (!this.navegador) return;
      if (token) localStorage.setItem(CLAVE_TOKEN, token);
      else localStorage.removeItem(CLAVE_TOKEN);
    } catch {
      // Sin almacenamiento (modo privado estricto): la bolsa dura lo que dure la pestaña.
    }
  }
}
