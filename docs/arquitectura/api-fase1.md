# Contrato de la API de la Fase 1 (cuentas y catálogo administrativo)

Concreta el plan de desarrollo §7 para RF01–RF04. Lo implementan en paralelo el backend y el frontend; si algo cambia, se actualiza aquí primero. Complementa `api-autenticacion.md`.

Convenciones: base `/api/v1`, JSON, dinero como número decimal (`"precioBase": 189000.00`), fechas ISO-8601 en UTC. Errores en `application/problem+json` en español: `400` con `errores: [{campo, mensaje}]`, `401`, `403`, `404`, `409` con `detail` legible. Listados paginados: `?page=0&size=20&sort=campo,asc` → `{ contenido: T[], pagina, tamano, totalElementos, totalPaginas }`.

Fuente del modelo: `docs/diseno/analisis-y-diseno-nalu.pdf` (Tablas 11 y 12).

## 1. Registro (RF01)

`POST /auth/registro` (público)

```ts
interface RegistroRequest {
  nombre: string;              // obligatorio, máx. 60
  apellido: string;            // obligatorio, máx. 60
  correo: string;              // obligatorio, correo válido, máx. 120
  telefono: string;            // obligatorio, ^\+?[0-9]{7,15}$
  contrasena: string;          // 8 a 128 caracteres (RNF01)
  aceptaPolitica: boolean;     // debe ser true (Ley 1581, RNF05)
  aceptaComunicaciones: boolean;
}
```

- `201` con la misma `Sesion` y cookie `nalu_rt` que el login: el cliente queda autenticado.
- `409` si el correo ya está registrado: `"Ya existe una cuenta con ese correo"`.

**Bloqueo por fuerza bruta (§8.5):** si un correo acumula 5 intentos fallidos en los últimos 15 minutos (contados desde su último login correcto), `POST /auth/login` responde `429` con `"Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo."`. La ventana es deslizante: el bloqueo termina cuando el más antiguo de esos fallos cumple 15 minutos. Los intentos durante el bloqueo no se cuentan.

## 2. Perfil y direcciones (RF02, cliente autenticado)

```ts
interface Perfil {
  nombre: string; apellido: string; correo: string;   // correo: solo lectura en F1
  telefono: string; documento: string | null; aceptaComunicaciones: boolean;
}
type PerfilRequest = Omit<Perfil, 'correo'>;          // mismas reglas que el registro; documento máx. 20

interface Direccion {
  id: number; etiqueta: string; destinatario: string; departamento: string;
  ciudad: string; direccion: string; codigoPostal: string | null; telefono: string;
}
type DireccionRequest = Omit<Direccion, 'id'>;
// etiqueta ≤40 (p. ej. «Casa», «Oficina»), destinatario ≤120, departamento ≤60, ciudad ≤60,
// direccion ≤160, codigoPostal ≤12 opcional, telefono como en el registro. Todos obligatorios salvo codigoPostal.
```

| Método y ruta | Respuesta |
|---|---|
| `GET /cuenta/perfil` | `200 Perfil` |
| `PUT /cuenta/perfil` | `200 Perfil` |
| `GET /cuenta/direcciones` | `200 Direccion[]` (solo activas) |
| `POST /cuenta/direcciones` | `201 Direccion` |
| `PUT /cuenta/direcciones/{id}` | `200 Direccion` |
| `DELETE /cuenta/direcciones/{id}` | `204`. Se desactiva, no se borra (plan §9: puede estar en pedidos) |

Una dirección de otro cliente o inactiva responde `404`.

## 3. Catálogo: lectura pública

| Método y ruta | Respuesta |
|---|---|
| `GET /materiales` | `200 Material[]` ordenados por tipo |

```ts
interface Material {
  id: number; tipo: string; ley: string | null;    // «Plata» + «925», «Esmeralda» + null
  requiereCertificado: boolean; factorPrecio: number;
}
```

(El árbol público de categorías y el catálogo son de la Fase 2.)

## 4. Administración (rol ADMINISTRADOR, prefijo `/admin`)

### Categorías (RF03)

```ts
interface CategoriaAdmin {
  id: number; nombre: string; slug: string; descripcion: string | null;
  activa: boolean; idPadre: number | null; hijos: CategoriaAdmin[];
}
interface CategoriaRequest {
  nombre: string;              // obligatorio, máx. 80
  slug: string | null;         // opcional; si falta se genera del nombre (minúsculas, sin tildes, guiones). ^[a-z0-9]+(-[a-z0-9]+)*$, máx. 100
  descripcion: string | null;
  idPadre: number | null;
}
```

| Método y ruta | Respuesta |
|---|---|
| `GET /admin/categorias` | `200 CategoriaAdmin[]` (árbol completo, raíces ordenadas por nombre; incluye inactivas) |
| `POST /admin/categorias` | `201 CategoriaAdmin` |
| `PUT /admin/categorias/{id}` | `200 CategoriaAdmin` |
| `PATCH /admin/categorias/{id}/estado` | body `{ activa: boolean }` → `200 CategoriaAdmin` |

`409` si el slug ya existe o si `idPadre` crea un ciclo (la propia categoría o una descendiente). No hay borrado: se desactiva.

### Materiales

| Método y ruta | Respuesta |
|---|---|
| `POST /admin/materiales` | body `MaterialRequest` → `201 Material` |
| `PUT /admin/materiales/{id}` | body `MaterialRequest` → `200 Material` |

`MaterialRequest = Omit<Material, 'id'>`: tipo obligatorio ≤30, ley ≤20, factorPrecio > 0 con 3 decimales máx. `409` si ya existe la combinación tipo + ley.

### Productos (RF04)

```ts
type EstadoProducto = 'BORRADOR' | 'PUBLICADO' | 'ARCHIVADO';
type RolMaterial = 'METAL_PRINCIPAL' | 'PIEDRA' | 'SECUNDARIO';

interface ProductoResumen {
  id: number; sku: string; nombre: string; slug: string; categoria: string;
  precioBase: number; estado: EstadoProducto; existenciaTotal: number;
  imagenPrincipal: string | null;       // URL
}
interface ProductoAdmin {
  id: number; sku: string; nombre: string; slug: string; descripcion: string | null;
  idCategoria: number; precioBase: number; estado: EstadoProducto; fechaCreacion: string;
  materiales: { idMaterial: number; tipo: string; ley: string | null; rol: RolMaterial;
                pesoGramos: number | null; quilates: number | null }[];
  variantes: Variante[];
  imagenes: Imagen[];                   // ordenadas por `orden`
}
interface Variante {
  id: number; sku: string; talla: string | null; pesoGramos: number | null;
  precioAdicional: number; stockActual: number; stockReservado: number; activa: boolean;
}
interface Imagen { id: number; url: string; textoAlternativo: string; orden: number; principal: boolean }

interface ProductoRequest {
  sku: string;                // obligatorio, ^[A-Z0-9-]{3,40}$
  nombre: string;             // obligatorio, máx. 120
  slug: string | null;        // opcional, se genera del nombre; máx. 140
  descripcion: string | null;
  idCategoria: number;        // obligatorio
  precioBase: number;         // obligatorio, > 0, 2 decimales máx.
}
interface MaterialProductoRequest { idMaterial: number; rol: RolMaterial; pesoGramos: number | null; quilates: number | null }
interface VarianteNuevaRequest {
  sku: string;                // obligatorio, mismo formato que el del producto, único en todo el catálogo
  talla: string | null;       // máx. 20
  pesoGramos: number | null;  // > 0
  precioAdicional: number;    // ≥ 0
  existenciaInicial: number;  // ≥ 0, entero
}
interface VarianteRequest { talla: string | null; pesoGramos: number | null; precioAdicional: number; activa: boolean }
```

| Método y ruta | Respuesta |
|---|---|
| `GET /admin/productos?q&estado&idCategoria&page&size&sort` | `200 Pagina<ProductoResumen>` (`q` busca en nombre y SKU) |
| `GET /admin/productos/{id}` | `200 ProductoAdmin` |
| `POST /admin/productos` | `201 ProductoAdmin` (estado `BORRADOR`) |
| `PUT /admin/productos/{id}` | `200 ProductoAdmin` |
| `PUT /admin/productos/{id}/materiales` | body `MaterialProductoRequest[]` (reemplaza el conjunto) → `200 ProductoAdmin` |
| `POST /admin/productos/{id}/variantes` | body `VarianteNuevaRequest` → `201 ProductoAdmin` |
| `PUT /admin/productos/{id}/variantes/{idVariante}` | body `VarianteRequest` → `200 ProductoAdmin` |
| `POST /admin/productos/{id}/imagenes` | `multipart/form-data`: `archivo` (JPEG, PNG o WebP, máx. 5 MB), `textoAlternativo` (obligatorio, ≤160) → `201 ProductoAdmin` |
| `PATCH /admin/productos/{id}/imagenes/{idImagen}` | body `{ textoAlternativo, orden, principal }` → `200 ProductoAdmin` |
| `DELETE /admin/productos/{id}/imagenes/{idImagen}` | `200 ProductoAdmin` |
| `PATCH /admin/productos/{id}/estado` | body `{ estado: EstadoProducto }` → `200 ProductoAdmin` |

Reglas:

- SKU de producto y de variante únicos (`409` con `"El SKU X ya existe"`, CU-11 3a). Slug de producto único (`409`).
- Un material no puede repetirse en el mismo producto (`400`).
- La existencia inicial de una variante registra un movimiento de inventario `ENTRADA` con motivo `"Existencia inicial"` y el administrador que la creó. Después, la existencia **no** se edita por esta API (los movimientos manuales son la Fase 5).
- La primera imagen queda como principal. Marcar otra como principal desmarca las demás (plan §5.5). Al borrar la principal, pasa a serlo la de menor `orden`.
- **Publicar** exige: categoría activa, al menos un material, al menos una variante activa y al menos una imagen. Si falta algo: `409` con la lista de lo que falta en `detail`.
- `ARCHIVADO` oculta el producto sin borrarlo. Desde `ARCHIVADO` se puede volver a `BORRADOR`.
- Las imágenes se sirven públicamente en `GET /archivos/productos/{nombre}` (el valor de `url`). En desarrollo se guardan en disco (`NALU_ARCHIVOS_DIR`, por defecto `./uploads`); el almacenamiento de producción sigue pendiente (CLAUDE.md §12).

## 5. Criterio de aceptación de la fase

Un administrador crea un producto, le asocia **dos materiales** (p. ej. Plata 925 como `METAL_PRINCIPAL` y Cuarzo como `PIEDRA`), le define **tres variantes** de talla con existencia inicial, sube una imagen y lo **publica**. Se verifica con una prueba de integración del backend y a mano en el panel.
