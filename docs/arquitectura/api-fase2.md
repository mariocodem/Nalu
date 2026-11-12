# Contrato de la API de la Fase 2 (tienda pública y SEO)

Complementa `api-fase1.md`. Todo es público (sin token) salvo `/admin/seo/**`. Precios en pesos enteros calculados por el backend con la fórmula del ADR 003; el frontend nunca calcula precios.

## Catálogo (RF06)

`GET /productos?q&categoria&material&precioMin&precioMax&disponible&sort&page&size` → `Pagina<ProductoTarjeta>`

| Parámetro | Efecto |
|---|---|
| `q` | Texto en nombre o descripción (sin distinguir mayúsculas) |
| `categoria` | Slug; incluye sus subcategorías activas. Slug inexistente o inactivo → `404` |
| `material` | Id de material: piezas que lo contienen con cualquier rol |
| `precioMin`, `precioMax` | Sobre `precioDesde` (el precio más bajo entre las variantes activas) |
| `disponible=true` | Solo piezas con alguna variante activa con existencias libres |
| `sort` | `precioDesde,asc|desc`, `nombre,asc`, `fechaCreacion,desc` (por defecto) |
| `size` | Por defecto 24, máximo 100 |

```ts
interface ProductoTarjeta {
  slug: string; nombre: string; categoria: string;
  imagen: { url: string; textoAlternativo: string } | null;   // la principal
  material: string | null;                                     // el metal principal, p. ej. "Plata 925"
  precioDesde: number | null; agotado: boolean;
}
```

Solo aparecen productos `PUBLICADO` de categorías activas. No se exponen cantidades de existencias.

## Categorías

- `GET /categorias` → árbol de categorías activas `{ nombre, slug, descripcion, hijos[] }`.
- `GET /categorias/{slug}` → `{ nombre, slug, descripcion, migas[], hijos[], seo }`.

## Ficha (RF07)

`GET /productos/{slug}` → `FichaProducto` (404 si no está publicado o su categoría está inactiva):

```ts
interface FichaProducto {
  slug: string; nombre: string; descripcion: string | null;
  migas: { nombre: string; slug: string }[];                 // categoría raíz → categoría del producto
  imagenes: { url: string; textoAlternativo: string }[];     // la principal primero
  materiales: { nombre: string; rol: RolMaterial; pesoGramos: number | null; quilates: number | null; requiereCertificado: boolean }[];
  variantes: { id: number; talla: string | null; precio: number; disponible: boolean }[];   // solo activas
  precioDesde: number | null; agotado: boolean;
  seo: Seo;
}
interface Seo {
  titulo: string; descripcion: string; palabrasClave: string | null; urlCanonica: string;
  jsonLd: object;   // Schema.org Product + Offer (un precio) o AggregateOffer (varios), COP, InStock/OutOfStock
}
```

Las reseñas aprobadas se añaden a la ficha en la Fase 5.

## SEO (RF05)

- `GET|PUT /admin/seo/productos/{id}` y `/admin/seo/categorias/{id}` con `{ titulo ≤160, metaDescripcion ≤320, palabrasClave ≤255, urlCanonica (URL completa), datosEstructurados (objeto, sin "@type") }`. Los campos vacíos se rellenan con el nombre y la descripción. `datosEstructurados` se mezcla con el JSON-LD generado, sin poder sustituir `@type` ni `offers`.
- `GET /seo/rutas` → `{ productos: string[], categorias: string[] }` (slugs) para el sitemap.
- El servidor SSR publica `/sitemap.xml` y `/robots.txt` (que excluye `/cuenta` y `/admin`). La URL pública se configura con `NALU_URL_PUBLICA` y la de la API vista desde el servidor con `NALU_API_URL`.
