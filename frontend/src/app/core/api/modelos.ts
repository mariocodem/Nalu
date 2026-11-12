// Tipos del contrato de la Fase 1 (docs/arquitectura/api-fase1.md).

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface RegistroRequest {
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  contrasena: string;
  aceptaPolitica: boolean;
  aceptaComunicaciones: boolean;
}

export interface Perfil {
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  documento: string | null;
  aceptaComunicaciones: boolean;
}
export type PerfilRequest = Omit<Perfil, 'correo'>;

export interface Direccion {
  id: number;
  etiqueta: string;
  destinatario: string;
  departamento: string;
  ciudad: string;
  direccion: string;
  codigoPostal: string | null;
  telefono: string;
}
export type DireccionRequest = Omit<Direccion, 'id'>;

export interface Material {
  id: number;
  tipo: string;
  ley: string | null;
  requiereCertificado: boolean;
  factorPrecio: number;
}
export type MaterialRequest = Omit<Material, 'id'>;

export interface CategoriaAdmin {
  id: number;
  nombre: string;
  slug: string;
  descripcion: string | null;
  activa: boolean;
  idPadre: number | null;
  hijos: CategoriaAdmin[];
}
export interface CategoriaRequest {
  nombre: string;
  slug: string | null;
  descripcion: string | null;
  idPadre: number | null;
}

export type EstadoProducto = 'BORRADOR' | 'PUBLICADO' | 'ARCHIVADO';
export type RolMaterial = 'METAL_PRINCIPAL' | 'PIEDRA' | 'SECUNDARIO';

export interface ProductoResumen {
  id: number;
  sku: string;
  nombre: string;
  slug: string;
  categoria: string;
  precioBase: number;
  estado: EstadoProducto;
  existenciaTotal: number;
  imagenPrincipal: string | null;
}
export interface Variante {
  id: number;
  sku: string;
  talla: string | null;
  pesoGramos: number | null;
  precioAdicional: number;
  stockActual: number;
  stockReservado: number;
  activa: boolean;
}
export interface Imagen {
  id: number;
  url: string;
  textoAlternativo: string;
  orden: number;
  principal: boolean;
}
export interface MaterialDeProducto {
  idMaterial: number;
  tipo: string;
  ley: string | null;
  rol: RolMaterial;
  pesoGramos: number | null;
  quilates: number | null;
}
export interface ProductoAdmin {
  id: number;
  sku: string;
  nombre: string;
  slug: string;
  descripcion: string | null;
  idCategoria: number;
  precioBase: number;
  estado: EstadoProducto;
  fechaCreacion: string;
  materiales: MaterialDeProducto[];
  variantes: Variante[];
  imagenes: Imagen[];
}
export interface ProductoRequest {
  sku: string;
  nombre: string;
  slug: string | null;
  descripcion: string | null;
  idCategoria: number;
  precioBase: number;
}
export interface MaterialProductoRequest {
  idMaterial: number;
  rol: RolMaterial;
  pesoGramos: number | null;
  quilates: number | null;
}
export interface VarianteNuevaRequest {
  sku: string;
  talla: string | null;
  pesoGramos: number | null;
  precioAdicional: number;
  existenciaInicial: number;
}
export interface VarianteRequest {
  talla: string | null;
  pesoGramos: number | null;
  precioAdicional: number;
  activa: boolean;
}

export const NOMBRES_ESTADO: Record<EstadoProducto, string> = {
  BORRADOR: 'Borrador',
  PUBLICADO: 'Publicado',
  ARCHIVADO: 'Archivado',
};
export const NOMBRES_ROL: Record<RolMaterial, string> = {
  METAL_PRINCIPAL: 'Metal principal',
  PIEDRA: 'Piedra',
  SECUNDARIO: 'Secundario',
};

export function nombreMaterial(m: { tipo: string; ley: string | null }): string {
  return m.ley ? `${m.tipo} ${m.ley}` : m.tipo;
}
