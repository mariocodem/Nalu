import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Sesion } from '../auth/sesion.model';
import { Direccion, DireccionRequest, Perfil, PerfilRequest, RegistroRequest } from './modelos';

/** Registro, perfil y direcciones del cliente (api-fase1.md §1 y §2). */
@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly http = inject(HttpClient);

  registrar(datos: RegistroRequest): Observable<Sesion> {
    // withCredentials: la respuesta deja la cookie del refresh token.
    return this.http.post<Sesion>('/api/v1/auth/registro', datos, { withCredentials: true });
  }

  perfil(): Observable<Perfil> {
    return this.http.get<Perfil>('/api/v1/cuenta/perfil');
  }

  actualizarPerfil(datos: PerfilRequest): Observable<Perfil> {
    return this.http.put<Perfil>('/api/v1/cuenta/perfil', datos);
  }

  direcciones(): Observable<Direccion[]> {
    return this.http.get<Direccion[]>('/api/v1/cuenta/direcciones');
  }

  crearDireccion(datos: DireccionRequest): Observable<Direccion> {
    return this.http.post<Direccion>('/api/v1/cuenta/direcciones', datos);
  }

  actualizarDireccion(id: number, datos: DireccionRequest): Observable<Direccion> {
    return this.http.put<Direccion>(`/api/v1/cuenta/direcciones/${id}`, datos);
  }

  eliminarDireccion(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/cuenta/direcciones/${id}`);
  }
}
