import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CategoriaAdmin,
  CategoriaRequest,
  EstadoProducto,
  Material,
  MaterialProductoRequest,
  MaterialRequest,
  Pagina,
  ProductoAdmin,
  ProductoRequest,
  ProductoResumen,
  VarianteNuevaRequest,
  VarianteRequest,
} from './modelos';

export interface PromocionRequest {
  codigo: string;
  tipo: 'PORCENTAJE' | 'VALOR_FIJO';
  valor: number;
  fechaInicio: string;
  fechaFin: string;
  usosMaximos: number | null;
  activa: boolean;
}
export interface Promocion extends PromocionRequest {
  id: number;
  usosActuales: number;
}

export interface SeoAdmin {
  titulo: string | null;
  metaDescripcion: string | null;
  palabrasClave: string | null;
  urlCanonica: string | null;
  datosEstructurados: Record<string, unknown> | null;
}

export interface FiltroProductos {
  q?: string;
  estado?: EstadoProducto | '';
  idCategoria?: number | null;
  page?: number;
  size?: number;
}

/** Panel de administración del catálogo (api-fase1.md §3 y §4). */
@Injectable({ providedIn: 'root' })
export class CatalogoAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin';

  categorias(): Observable<CategoriaAdmin[]> {
    return this.http.get<CategoriaAdmin[]>(`${this.base}/categorias`);
  }
  crearCategoria(datos: CategoriaRequest): Observable<CategoriaAdmin> {
    return this.http.post<CategoriaAdmin>(`${this.base}/categorias`, datos);
  }
  actualizarCategoria(id: number, datos: CategoriaRequest): Observable<CategoriaAdmin> {
    return this.http.put<CategoriaAdmin>(`${this.base}/categorias/${id}`, datos);
  }
  cambiarEstadoCategoria(id: number, activa: boolean): Observable<CategoriaAdmin> {
    return this.http.patch<CategoriaAdmin>(`${this.base}/categorias/${id}/estado`, { activa });
  }

  materiales(): Observable<Material[]> {
    return this.http.get<Material[]>('/api/v1/materiales');
  }
  crearMaterial(datos: MaterialRequest): Observable<Material> {
    return this.http.post<Material>(`${this.base}/materiales`, datos);
  }
  actualizarMaterial(id: number, datos: MaterialRequest): Observable<Material> {
    return this.http.put<Material>(`${this.base}/materiales/${id}`, datos);
  }

  productos(filtro: FiltroProductos): Observable<Pagina<ProductoResumen>> {
    let params = new HttpParams()
      .set('page', String(filtro.page ?? 0))
      .set('size', String(filtro.size ?? 20));
    if (filtro.q) params = params.set('q', filtro.q);
    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.idCategoria) params = params.set('idCategoria', String(filtro.idCategoria));
    return this.http.get<Pagina<ProductoResumen>>(`${this.base}/productos`, { params });
  }
  producto(id: number): Observable<ProductoAdmin> {
    return this.http.get<ProductoAdmin>(`${this.base}/productos/${id}`);
  }
  crearProducto(datos: ProductoRequest): Observable<ProductoAdmin> {
    return this.http.post<ProductoAdmin>(`${this.base}/productos`, datos);
  }
  actualizarProducto(id: number, datos: ProductoRequest): Observable<ProductoAdmin> {
    return this.http.put<ProductoAdmin>(`${this.base}/productos/${id}`, datos);
  }
  reemplazarMateriales(id: number, datos: MaterialProductoRequest[]): Observable<ProductoAdmin> {
    return this.http.put<ProductoAdmin>(`${this.base}/productos/${id}/materiales`, datos);
  }
  agregarVariante(id: number, datos: VarianteNuevaRequest): Observable<ProductoAdmin> {
    return this.http.post<ProductoAdmin>(`${this.base}/productos/${id}/variantes`, datos);
  }
  actualizarVariante(id: number, idVariante: number, datos: VarianteRequest): Observable<ProductoAdmin> {
    return this.http.put<ProductoAdmin>(`${this.base}/productos/${id}/variantes/${idVariante}`, datos);
  }
  subirImagen(id: number, archivo: File, textoAlternativo: string): Observable<ProductoAdmin> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    datos.append('textoAlternativo', textoAlternativo);
    return this.http.post<ProductoAdmin>(`${this.base}/productos/${id}/imagenes`, datos);
  }
  actualizarImagen(
    id: number,
    idImagen: number,
    datos: { textoAlternativo: string; orden: number; principal: boolean },
  ): Observable<ProductoAdmin> {
    return this.http.patch<ProductoAdmin>(`${this.base}/productos/${id}/imagenes/${idImagen}`, datos);
  }
  eliminarImagen(id: number, idImagen: number): Observable<ProductoAdmin> {
    return this.http.delete<ProductoAdmin>(`${this.base}/productos/${id}/imagenes/${idImagen}`);
  }
  promociones(): Observable<Promocion[]> {
    return this.http.get<Promocion[]>(`${this.base}/promociones`);
  }
  crearPromocion(datos: PromocionRequest): Observable<Promocion> {
    return this.http.post<Promocion>(`${this.base}/promociones`, datos);
  }
  actualizarPromocion(id: number, datos: PromocionRequest): Observable<Promocion> {
    return this.http.put<Promocion>(`${this.base}/promociones/${id}`, datos);
  }
  seo(tipo: 'productos' | 'categorias', id: number): Observable<SeoAdmin> {
    return this.http.get<SeoAdmin>(`${this.base}/seo/${tipo}/${id}`);
  }
  guardarSeo(tipo: 'productos' | 'categorias', id: number, datos: SeoAdmin): Observable<SeoAdmin> {
    return this.http.put<SeoAdmin>(`${this.base}/seo/${tipo}/${id}`, datos);
  }
  cambiarEstado(id: number, estado: EstadoProducto): Observable<ProductoAdmin> {
    return this.http.patch<ProductoAdmin>(`${this.base}/productos/${id}/estado`, { estado });
  }
}
