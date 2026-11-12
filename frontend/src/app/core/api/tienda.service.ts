import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Material, Pagina } from './modelos';

// Contrato: docs/arquitectura/api-fase2.md
export interface ImagenPublica { url: string; textoAlternativo: string }
export interface ProductoTarjeta {
  slug: string; nombre: string; categoria: string; imagen: ImagenPublica | null;
  material: string | null; precioDesde: number | null; agotado: boolean;
}
export interface CategoriaPublica { nombre: string; slug: string; descripcion: string | null; hijos: CategoriaPublica[] }
export interface Miga { nombre: string; slug: string }
export interface SeoPublico {
  titulo: string; descripcion: string; palabrasClave: string | null; urlCanonica: string;
  jsonLd: Record<string, unknown>;
}
export interface CategoriaDetalle {
  nombre: string; slug: string; descripcion: string | null; migas: Miga[]; hijos: CategoriaPublica[]; seo: SeoPublico;
}
export interface MaterialFicha {
  nombre: string; rol: 'METAL_PRINCIPAL' | 'PIEDRA' | 'SECUNDARIO'; pesoGramos: number | null;
  quilates: number | null; requiereCertificado: boolean;
}
export interface VarianteFicha { id: number; talla: string | null; precio: number; disponible: boolean }
export interface FichaProducto {
  slug: string; nombre: string; descripcion: string | null; migas: Miga[]; imagenes: ImagenPublica[];
  materiales: MaterialFicha[]; variantes: VarianteFicha[]; precioDesde: number | null; agotado: boolean;
  resenas: { promedio: number | null; total: number; resenas: { autor: string; calificacion: number; comentario: string | null; fecha: string }[] };
  seo: SeoPublico;
}
export interface FiltroCatalogo {
  q?: string | null; categoria?: string | null; material?: number | null;
  precioMin?: number | null; precioMax?: number | null; disponible?: boolean | null;
  sort?: string | null; page?: number; size?: number;
}

@Injectable({ providedIn: 'root' })
export class TiendaService {
  private readonly http = inject(HttpClient);

  categorias(): Observable<CategoriaPublica[]> {
    return this.http.get<CategoriaPublica[]>('/api/v1/categorias');
  }

  categoria(slug: string): Observable<CategoriaDetalle> {
    return this.http.get<CategoriaDetalle>(`/api/v1/categorias/${encodeURIComponent(slug)}`);
  }

  materiales(): Observable<Material[]> {
    return this.http.get<Material[]>('/api/v1/materiales');
  }

  productos(filtro: FiltroCatalogo): Observable<Pagina<ProductoTarjeta>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== null && valor !== undefined && valor !== '' && valor !== false) {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<ProductoTarjeta>>('/api/v1/productos', { params });
  }

  ficha(slug: string): Observable<FichaProducto> {
    return this.http.get<FichaProducto>(`/api/v1/productos/${encodeURIComponent(slug)}`);
  }
}
