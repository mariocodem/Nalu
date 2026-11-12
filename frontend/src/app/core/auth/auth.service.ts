import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformServer } from '@angular/common';
import {
  Observable,
  catchError,
  defer,
  finalize,
  firstValueFrom,
  map,
  of,
  shareReplay,
  tap,
} from 'rxjs';
import { Sesion, UsuarioSesion } from './sesion.model';

/**
 * Estado de autenticación de la aplicación.
 *
 * <p>El access token y el usuario viven solo en memoria (signals); nunca se usan
 * localStorage ni sessionStorage. El refresh token viaja en una cookie HttpOnly que
 * administra el navegador.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private static readonly URL_BASE = '/api/v1/auth';
  static readonly BLOQUEO_REFRESH = 'nalu-auth-refresh';

  private readonly http = inject(HttpClient);
  private readonly plataforma = inject(PLATFORM_ID);

  private readonly _usuario = signal<UsuarioSesion | null>(null);
  private readonly _accessToken = signal<string | null>(null);

  /** Petición de refresh compartida mientras está en vuelo. */
  private refrescoEnVuelo$: Observable<Sesion> | null = null;

  /** Usuario autenticado o {@code null}. */
  readonly usuario = this._usuario.asReadonly();

  /** Access token en memoria o {@code null}. */
  readonly accessToken = this._accessToken.asReadonly();

  /** Indica si hay un access token en memoria. */
  readonly autenticado = computed(() => this._accessToken() !== null);

  /** Indica si el usuario tiene el rol ADMINISTRADOR. */
  readonly esAdministrador = computed(
    () => this._usuario()?.roles.includes('ADMINISTRADOR') ?? false,
  );

  /**
   * Inicia sesión contra la API y guarda la sesión recibida.
   *
   * @param correo     correo electrónico
   * @param contrasena contraseña en claro (solo viaja por HTTPS)
   * @returns datos públicos del usuario autenticado
   */
  login(correo: string, contrasena: string): Observable<UsuarioSesion> {
    return this.http
      .post<Sesion>(`${AuthService.URL_BASE}/login`, { correo, contrasena })
      .pipe(
        tap((sesion) => this.guardarSesion(sesion)),
        map((sesion) => sesion.usuario),
      );
  }

  /**
   * Renueva la sesión usando la cookie de refresh.
   *
   * <p>Las llamadas concurrentes comparten una única petición en vuelo. La cookie
   * se envía con {@code withCredentials: true}.
   */
  refrescar(): Observable<Sesion> {
    if (this.refrescoEnVuelo$ === null) {
      this.refrescoEnVuelo$ = this.conBloqueoEntrePestanas(
        this.http.post<Sesion>(`${AuthService.URL_BASE}/refresh`, null, { withCredentials: true }),
      ).pipe(
          tap((sesion) => this.guardarSesion(sesion)),
          finalize(() => {
            this.refrescoEnVuelo$ = null;
          }),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }

    return this.refrescoEnVuelo$;
  }

  /**
   * Cierra la sesión en el servidor y limpia siempre el estado local.
   *
   * <p>Si la petición falla, el estado local también se limpia y el error se
   * absorbe para que el cierre de sesión no bloquee la interfaz.
   */
  logout(): Observable<void> {
    return this.http
      .post<void>(`${AuthService.URL_BASE}/logout`, null, { withCredentials: true })
      .pipe(
        catchError(() => of(undefined)),
        finalize(() => this.limpiarSesion()),
      );
  }

  /** Guarda una sesión obtenida fuera de login/refresh (p. ej. al registrarse). */
  establecerSesion(sesion: Sesion): void {
    this.guardarSesion(sesion);
  }

  /** Borra el access token y el usuario de la memoria. */
  limpiarSesion(): void {
    this._accessToken.set(null);
    this._usuario.set(null);
  }

  /**
   * Intenta recuperar la sesión al arrancar la aplicación.
   *
   * <p>En el servidor no se hace nada: la cookie HttpOnly no viaja al renderizador
   * de Angular y una llamada a la API durante el SSR no aportaría sesión. En el
   * navegador se prueba el refresh y un fallo (por ejemplo 401) deja la app sin
   * sesión sin romper el arranque.
   */
  async restaurarSesion(): Promise<void> {
    if (isPlatformServer(this.plataforma)) {
      return;
    }

    try {
      await firstValueFrom(this.refrescar());
    } catch {
      this.limpiarSesion();
    }
  }

  /**
   * Serializa el refresh entre pestañas con la Web Locks API.
   *
   * <p>Todas las pestañas comparten la cookie del refresh token. Si dos pestañas la envían a la
   * vez (p. ej. al restaurar varias pestañas), el backend ve la segunda como una reutilización y
   * revoca toda la sesión. Con el bloqueo, la segunda espera a que termine la primera y sale con
   * la cookie ya rotada. Sin Web Locks (SSR, navegadores antiguos) se envía sin bloqueo.
   */
  private conBloqueoEntrePestanas(peticion$: Observable<Sesion>): Observable<Sesion> {
    const locks = typeof navigator === 'undefined' ? undefined : navigator.locks;
    if (!locks) {
      return peticion$;
    }
    return defer(() => locks.request(AuthService.BLOQUEO_REFRESH, () => firstValueFrom(peticion$)));
  }

  private guardarSesion(sesion: Sesion): void {
    this._accessToken.set(sesion.accessToken);
    this._usuario.set(sesion.usuario);
  }
}
