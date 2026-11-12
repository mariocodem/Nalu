# Nalu — Plan de desarrollo del MVP

> Documento de arranque técnico del sistema transaccional de comercio electrónico **Nalu**.
> Fuente de verdad funcional: *Análisis y Diseño del Sistema Transaccional de Comercio Electrónico Nalu* (Sistemas Transaccionales, Grupo 9 — UMB, 2026).
> Este archivo traduce ese diseño en decisiones de implementación concretas. Si algo aquí contradice el documento de diseño, se documenta en la sección **"Ajustes técnicos respecto al diseño"**.

---

## 1. Resumen del producto

Tienda en línea + panel administrativo para una joyería multimaterial (acero, plata, oro, esmeraldas, cuarzo) que cubre el ciclo completo de una venta:

**catálogo → carrito → pedido con reserva de existencias → pago por pasarela (Wompi sandbox) → gestión del pedido y envío → reportes.**

Fuera del alcance del MVP: pagos reales en producción, integración automática con transportadoras (la guía se registra a mano), facturación electrónica, marketplaces, fidelización, app móvil nativa y el servicio de IA (apartado 13, solo conceptual).

---

## 2. Stack tecnológico

| Capa | Tecnología | Notas |
|---|---|---|
| Backend | Java LTS (21 o 25) + Spring Boot | Spring Web, Spring Security, Spring Mail, Actuator |
| ORM | Spring Data JPA + Hibernate | Entidades como fuente de verdad del modelo; consultas derivadas, Specifications y JPQL; **sin SQL nativo** |
| Validación de entrada | **YAVI** | Esquemas declarados en código, estilo Zod (sección 4.6) |
| Autenticación | **JWT** (Spring Security OAuth2 Resource Server + Nimbus) | Access token de 15 min + refresh token rotativo en cookie `HttpOnly` (sección 8) |
| Frontend | Angular (versión estable actual) con SSR | Componentes *standalone*, `@angular/ssr`, Signals |
| Base de datos | PostgreSQL (16+) | Transacciones ACID, `JSONB`; accedida solo a través de Hibernate |
| Versionado del esquema | Liquibase (changelogs YAML) | Changesets generados por *diff* desde las entidades; `ddl-auto=validate` |
| Pruebas | JUnit 5, Mockito, Testcontainers, JaCoCo | Cobertura ≥ 60 % en pedidos, inventario y promociones (RNF13) |
| Pagos | Wompi (entorno sandbox) | Widget/Checkout web + webhook de eventos firmado |
| Correo (dev) | Mailpit | Captura los correos de notificación en local |
| Contenedores | Docker Compose | PostgreSQL + Mailpit en local |
| CI | GitHub Actions | Build + pruebas en cada *push* / PR |

> Antes de iniciar, fijar las versiones exactas en `pom.xml` y `package.json` y anotarlas aquí.

---

## 3. Estructura del repositorio (monorepo)

```
nalu-ecommerce/
├── backend/                     # Spring Boot (API REST)
│   ├── src/main/java/co/nalu/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── db/changelog/        # db.changelog-master.yaml + changesets YAML generados desde las entidades
│   └── src/test/java/co/nalu/
├── frontend/                    # Angular (tienda + panel admin, SSR)
├── docs/
│   ├── diseno/                  # PDF del análisis y diseño, diagramas
│   └── decisiones/              # ADR: decisiones de arquitectura y de negocio
├── docker-compose.yml
├── .env.example
├── .github/workflows/ci.yml
└── README.md
```

---

## 4. Arquitectura por capas

El sistema implementa la **arquitectura en tres capas cliente-servidor** del apartado 4 del diseño. La comunicación es siempre **descendente**: cada capa solo invoca a la inmediatamente inferior y nunca a una superior.

```
┌──────────────────────────────────────────────────────────────┐
│ CAPA DE PRESENTACIÓN — frontend/ (Angular + SSR)             │
│   Tienda en línea · Panel administrativo                     │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTPS / JSON (API REST)
┌───────────────────────────▼──────────────────────────────────┐
│ CAPA DE LÓGICA DE NEGOCIO — backend/                         │
│   api/         Controlador de solicitudes (REST, JWT, YAVI)  │
│      │                                                       │
│   negocio/     Módulos: catálogo, inventario, pedidos, ...   │
│      │  ▲                                                    │
│      │  └── integracion/  Wompi, correo (servicios externos) │
└──────┼───────────────────────────────────────────────────────┘
       │
┌──────▼───────────────────────────────────────────────────────┐
│ CAPA DE DATOS — backend/datos/                               │
│   ORM Hibernate: entidades JPA · repositorios · archivos     │
│                 │                                            │
│            PostgreSQL (transacciones ACID)                   │
└──────────────────────────────────────────────────────────────┘
```

### 4.1 Paquetes del backend

Los paquetes de primer nivel son las **capas**; dentro de cada capa, los **módulos** del diseño (catálogo, inventario, pedidos, etc.).

```
co.nalu
├── NaluApplication.java
│
├── api/                        # ── Controlador de solicitudes (entrada a la capa de negocio)
│   ├── publico/                #    Catálogo, categorías, auth, carrito de visitante
│   ├── cliente/                #    Perfil, direcciones, checkout, mis pedidos, reseñas
│   ├── admin/                  #    Rutas /admin/** (rol ADMINISTRADOR)
│   ├── webhook/                #    WompiWebhookController
│   └── error/                  #    @RestControllerAdvice → problem+json en español
│
├── negocio/                    # ── Lógica de negocio (reglas del dominio y transacciones)
│   ├── cuentas/                #    Registro, login, emisión/rotación de JWT, administrador inicial
│   ├── clientes/               #    Perfil, direcciones, solicitud de supresión
│   ├── catalogo/               #    Productos, materiales, variantes, CalculadoraPrecio
│   ├── inventario/             #    Reserva, liberación, movimientos, disponibilidad
│   ├── carrito/                #    Ítems, fusión visitante → cliente
│   ├── pedidos/                #    CheckoutService, GestionPedidoService, EstadoPedido, expiración
│   ├── pagos/                  #    PagoService, procesamiento de eventos, puerto PasarelaPagos
│   ├── envios/                 #    Registro de guía y estado de entrega
│   ├── promociones/            #    Validación de cupones y cálculo de descuento
│   ├── contenido/              #    Metadatos SEO, JSON-LD, moderación de reseñas
│   ├── reportes/               #    Consultas agregadas, exportación CSV
│   ├── notificaciones/         #    Puerto Notificador y eventos de dominio
│   └── comun/                  #    Excepciones de negocio, auditoría, utilidades de dinero
│
├── datos/                      # ── Capa de datos (persistencia)
│   ├── entidad/                #    @Entity (Hibernate) y enums persistidos, una por tabla
│   ├── repositorio/            #    Interfaces Spring Data JPA y consultas de reportes
│   └── archivo/                #    AlmacenamientoArchivos (disco local en dev, bucket en despliegue)
│
├── integracion/                # ── Adaptadores de servicios externos
│   ├── wompi/                  #    WompiPasarelaPagos implements negocio.pagos.PasarelaPagos
│   └── correo/                 #    CorreoNotificador implements negocio.notificaciones.Notificador
│
└── config/                     # Seguridad JWT, CORS, scheduling, @ConfigurationProperties
```

Cada módulo de `negocio/` tiene la misma forma:

```
negocio/pedidos/
├── CheckoutService.java        # @Service, @Transactional
├── GestionPedidoService.java
├── EstadoPedido.java           # máquina de estados (regla de negocio)
├── dto/                        # records de entrada (cada uno con su ESQUEMA YAVI) y de salida
└── mapper/                     # entidad ↔ dto
```

### 4.2 Responsabilidades y reglas de dependencia

| Capa / paquete | Responsabilidad | Puede usar | No puede usar |
|---|---|---|---|
| **Presentación** (Angular) | Interfaces de tienda y panel; validación de formularios para UX | API REST | Base de datos, lógica de precios o stock |
| **`api`** | Recibir HTTP, leer el JWT, validar la entrada con YAVI, delegar al servicio, traducir excepciones a códigos HTTP | `negocio` | `datos` (ni entidades ni repositorios) |
| **`negocio`** | Reglas del dominio: precios, disponibilidad, reservas, estados, cupones; demarcar transacciones (`@Transactional`) | `datos`, sus propios puertos (interfaces) | `api`, `integracion` (solo vía interfaz) |
| **`integracion`** | Hablar con Wompi y el servidor de correo | Interfaces de `negocio` que implementa | `api`, `datos` |
| **`datos`** | Mapeo objeto-relacional con Hibernate, repositorios, bloqueos de fila, archivos | Nada de las capas superiores | `api`, `negocio` |

Reglas concretas:
1. **Los controladores nunca reciben ni devuelven entidades JPA.** Usan los `dto` del módulo de negocio; el mapeo entidad ↔ dto ocurre dentro de `negocio`.
2. **`@Transactional` solo en servicios de `negocio`**, nunca en controladores ni repositorios. La confirmación atómica del pedido (RNF10) vive en `CheckoutService`.
3. **Entre módulos de negocio se colabora por servicio, no por repositorio ajeno**: `CheckoutService` llama a `InventarioService.reservar(...)`, no a `VarianteProductoRepository` directamente.
4. **Inversión de dependencias para servicios externos**: `negocio` define `PasarelaPagos` y `Notificador`; `integracion` los implementa. Cambiar Wompi por otra pasarela no toca la lógica de negocio (misma idea del adaptador de IA del apartado 13).
5. **El frontend no calcula nada que afecte el dinero o el stock**; muestra lo que el backend devuelve.

### 4.3 Recorrido de una solicitud (ejemplo: crear pedido)

```
Angular CheckoutComponent
  └─ POST /api/v1/pedidos ───────────────► api.cliente.PedidoController
     Authorization: Bearer <JWT>             ├─ validar(req, CrearPedidoRequest.ESQUEMA)   [YAVI → 400 si falla]
                                             └─ negocio.pedidos.CheckoutService.crearPedido(cmd)   [@Transactional]
                                                  ├─ negocio.inventario.InventarioService.reservar(...)
                                                  │    └─ datos.repositorio.VarianteProductoRepository (FOR UPDATE)
                                                  ├─ negocio.promociones.PromocionService.aplicar(...)
                                                  ├─ datos.repositorio.PedidoRepository.save(...)
                                                  └─ negocio.pagos.PagoService.iniciar(...)
                                                       └─ PasarelaPagos ◄── integracion.wompi.WompiPasarelaPagos
  ◄─ 201 PedidoCreadoDto (número, total, datos del widget)
```

### 4.4 Verificación automática de las capas (ArchUnit)

`backend/src/test/java/co/nalu/ArquitecturaCapasTest.java` — falla el build si alguien rompe la dirección de las dependencias (aporta a RNF13):

```java
@AnalyzeClasses(packages = "co.nalu", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaCapasTest {

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("API").definedBy("co.nalu.api..")
            .layer("Negocio").definedBy("co.nalu.negocio..")
            .layer("Integracion").definedBy("co.nalu.integracion..")
            .layer("Datos").definedBy("co.nalu.datos..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Negocio").mayOnlyBeAccessedByLayers("API", "Integracion")
            .whereLayer("Integracion").mayNotBeAccessedByAnyLayer()
            .whereLayer("Datos").mayOnlyBeAccessedByLayers("Negocio");

    @ArchTest
    static final ArchRule transaccionesSoloEnNegocio = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("co.nalu.negocio..");

    @ArchTest
    static final ArchRule controladoresSinEntidades = noClasses()
            .that().resideInAPackage("co.nalu.api..")
            .should().dependOnClassesThat().resideInAPackage("co.nalu.datos..");

    // ORM obligatorio: nada de SQL a mano
    @ArchTest
    static final ArchRule sinJdbcDirecto = noClasses()
            .should().dependOnClassesThat().resideInAPackage("org.springframework.jdbc.core..");

    @ArchTest
    static final ArchRule sinConsultasNativasEnEntityManager = noClasses()
            .should().callMethod(EntityManager.class, "createNativeQuery", String.class)
            .orShould().callMethod(EntityManager.class, "createNativeQuery", String.class, Class.class);

    @ArchTest
    static final ArchRule sinNativeQuery = methods()
            .that().areAnnotatedWith(Query.class)
            .should(new ArchCondition<JavaMethod>("no usar nativeQuery = true") {
                @Override
                public void check(JavaMethod metodo, ConditionEvents eventos) {
                    if (metodo.getAnnotationOfType(Query.class).nativeQuery()) {
                        eventos.add(SimpleConditionEvent.violated(metodo,
                                metodo.getFullName() + " usa SQL nativo"));
                    }
                }
            });
}
```

Dependencia: `com.tngtech.archunit:archunit-junit5` (scope `test`).

### 4.5 Capas en el frontend

Angular sigue la misma idea dentro de la capa de presentación:

| Carpeta | Rol | Regla |
|---|---|---|
| `tienda/`, `cuenta/`, `admin/` | Componentes de página (vista) | Solo hablan con servicios de `core/api` |
| `shared/` | Componentes visuales reutilizables | Sin llamadas HTTP |
| `core/api/` | Servicios que consumen la API REST | Única carpeta que usa `HttpClient` |
| `core/auth/` | `AuthService`, interceptor JWT y guards de rol | Ver sección 8.6 |
| `core/modelos/` | Interfaces TypeScript de los DTO | Espejo de los `dto` del backend |

### 4.6 Validación de entrada con YAVI (esquemas tipo Zod)

Para validar lo que llega a la API se usa **YAVI** (*Yet Another Validation for Java*), la librería más cercana a Zod en el ecosistema Java: el esquema se declara **en código, con lambdas y referencias a métodos** (sin anotaciones ni reflexión), es *type-safe*, soporta reglas entre campos, objetos anidados y colecciones, y devuelve todos los errores juntos.

| Zod (TypeScript) | YAVI (Java) |
|---|---|
| `z.object({ ... })` | `ValidatorBuilder.<T>of() ... .build()` |
| `z.string().min(1).max(60)` | `._string(T::campo, "campo", c -> c.notBlank().lessThanOrEqual(60))` |
| `z.number().int().positive()` | `._integer(T::cantidad, "cantidad", c -> c.notNull().greaterThan(0))` |
| `.email()` | `c.email()` |
| `.refine((o) => ..., { path: [...] })` | `.constraintOnTarget(o -> ..., "campo", "clave", "mensaje")` |
| `z.array(itemSchema).min(1)` | `.forEach(T::items, "items", itemValidator)` + `._collection(...notEmpty())` |
| `schema.safeParse(x)` | `validator.validate(x)` → `ConstraintViolations` |

**Dónde vive el esquema:** junto al DTO de entrada, como constante del `record` (igual que un esquema Zod junto a su tipo). Así el DTO y sus reglas no se separan.

```java
// negocio/cuentas/dto/RegistroClienteRequest.java
public record RegistroClienteRequest(
        String nombre, String apellido, String correo,
        String telefono, String contrasena, boolean aceptaPolitica) {

    public static final Validator<RegistroClienteRequest> ESQUEMA =
        ValidatorBuilder.<RegistroClienteRequest>of()
            ._string(RegistroClienteRequest::nombre, "nombre",
                     c -> c.notBlank().message("El nombre es obligatorio")
                           .lessThanOrEqual(60))
            ._string(RegistroClienteRequest::apellido, "apellido",
                     c -> c.notBlank().lessThanOrEqual(60))
            ._string(RegistroClienteRequest::correo, "correo",
                     c -> c.notBlank().email().message("Ingresa un correo válido")
                           .lessThanOrEqual(120))
            ._string(RegistroClienteRequest::telefono, "telefono",
                     c -> c.notBlank().pattern("^\\+?[0-9]{7,15}$")
                           .message("El teléfono debe tener entre 7 y 15 dígitos"))
            ._string(RegistroClienteRequest::contrasena, "contrasena",
                     c -> c.notBlank().greaterThanOrEqual(8)
                           .message("La contraseña debe tener al menos 8 caracteres"))   // RNF01
            .constraintOnTarget(RegistroClienteRequest::aceptaPolitica, "aceptaPolitica",
                     "politica.requerida", "Debes aceptar la política de tratamiento de datos") // RNF05
            .build();
}
```

**Esquemas compuestos (pedido con ítems):**

```java
public record ItemPedidoRequest(Long idVariante, Integer cantidad) {
    public static final Validator<ItemPedidoRequest> ESQUEMA =
        ValidatorBuilder.<ItemPedidoRequest>of()
            ._long(ItemPedidoRequest::idVariante, "idVariante", c -> c.notNull().greaterThan(0L))
            ._integer(ItemPedidoRequest::cantidad, "cantidad",
                      c -> c.notNull().greaterThan(0).lessThanOrEqual(20))
            .build();
}

public record CrearPedidoRequest(Long idDireccion, String codigoCupon, List<ItemPedidoRequest> items) {
    public static final Validator<CrearPedidoRequest> ESQUEMA =
        ValidatorBuilder.<CrearPedidoRequest>of()
            ._long(CrearPedidoRequest::idDireccion, "idDireccion", c -> c.notNull())
            ._string(CrearPedidoRequest::codigoCupon, "codigoCupon",
                     c -> c.lessThanOrEqual(30).pattern("^[A-Z0-9-]*$"))
            ._collection(CrearPedidoRequest::items, "items",
                     c -> c.notEmpty().message("El pedido debe tener al menos un ítem"))
            .forEach(CrearPedidoRequest::items, "items", ItemPedidoRequest.ESQUEMA)
            .build();
}

// Regla entre campos (equivalente a .refine de Zod)
public record PromocionRequest(String codigo, TipoPromocion tipo, BigDecimal valor,
                               LocalDate fechaInicio, LocalDate fechaFin, Integer usosMaximos) {
    public static final Validator<PromocionRequest> ESQUEMA =
        ValidatorBuilder.<PromocionRequest>of()
            ._string(PromocionRequest::codigo, "codigo",
                     c -> c.notBlank().lessThanOrEqual(30).pattern("^[A-Z0-9-]+$"))
            ._object(PromocionRequest::tipo, "tipo", c -> c.notNull())
            ._bigDecimal(PromocionRequest::valor, "valor", c -> c.notNull().greaterThan(BigDecimal.ZERO))
            ._object(PromocionRequest::fechaInicio, "fechaInicio", c -> c.notNull())
            ._object(PromocionRequest::fechaFin, "fechaFin", c -> c.notNull())
            .constraintOnTarget(p -> p.tipo() != TipoPromocion.PORCENTAJE || p.valor() == null
                                     || p.valor().compareTo(BigDecimal.valueOf(100)) <= 0,
                     "valor", "porcentaje.maximo", "Un porcentaje no puede superar 100")
            .constraintOnTarget(p -> p.fechaInicio() == null || p.fechaFin() == null
                                     || !p.fechaFin().isBefore(p.fechaInicio()),
                     "fechaFin", "rango.fechas", "La fecha final debe ser posterior a la inicial")
            .build();
}
```

**Cómo se aplica:** el controlador valida antes de delegar al servicio (la forma de los datos se valida en la entrada; las reglas que requieren la base de datos —stock, cupón vigente, SKU único— se validan en `negocio`).

```java
// api/error/Validacion.java
public final class Validacion {
    public static <T> T validar(T dto, Validator<T> esquema) {
        esquema.validate(dto).throwIfInvalid(EntradaInvalidaException::new);
        return dto;
    }
}

// api/cliente/PedidoController.java
@PostMapping("/pedidos")
public ResponseEntity<PedidoCreadoDto> crear(@RequestBody CrearPedidoRequest req,
                                             @AuthenticationPrincipal Jwt jwt) {
    validar(req, CrearPedidoRequest.ESQUEMA);
    var pedido = checkoutService.crearPedido(idUsuario(jwt), req);
    return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
}
```

El `@RestControllerAdvice` convierte `EntradaInvalidaException` en **400** `problem+json` con todos los errores por campo (RNF08, RNF11):

```json
{
  "type": "https://nalu.co/errores/validacion",
  "title": "Datos inválidos",
  "status": 400,
  "errores": [
    { "campo": "correo", "mensaje": "Ingresa un correo válido" },
    { "campo": "items[0].cantidad", "mensaje": "\"items[0].cantidad\" must be greater than 0" }
  ]
}
```

> Los mensajes por defecto de YAVI están en inglés; se personalizan con `.message(...)` en cada restricción o con un `MessageFormatter` propio que lea `messages_es.properties`. Definir esto en F0 para que todos los mensajes salgan en español.

**Dos niveles de validación, sin duplicar intención:**

| Nivel | Herramienta | Qué valida |
|---|---|---|
| Entrada de la API (`negocio/*/dto`) | **YAVI** | Forma y formato de lo que envía el cliente |
| Entidades (`datos/entidad`) | Bean Validation (incluida con Hibernate) | Invariantes de persistencia; última barrera antes del `INSERT`/`UPDATE` (sección 5) |

En el frontend se puede usar **Zod** en los formularios de Angular con los mismos límites, solo para mejorar la experiencia; la validación que cuenta es la del backend.

### 4.7 Convenciones

- DTO como `record` dentro de `negocio/<modulo>/dto`; nunca exponer entidades JPA en la API.
- Validación de entrada con los esquemas YAVI de cada DTO (4.6) + reglas de negocio en el servicio (RNF11).
- Errores con un `@RestControllerAdvice` que responde en formato `application/problem+json` y mensajes en español (RNF08).
- Dinero siempre en `BigDecimal` (`precision = 12, scale = 2`). Nunca `double`.
- Fechas en `OffsetDateTime` / `TIMESTAMPTZ`, zona `America/Bogota` para presentación.

---

## 5. Modelo de datos con ORM (JPA / Hibernate)

**Principio:** las entidades JPA de `datos/entidad` son la fuente de verdad del modelo. No se escriben scripts SQL a mano ni consultas nativas. Hibernate mapea las clases del diagrama de clases (apartado 6 del diseño) a las tablas del modelo relacional (apartado 7).

### 5.1 Herramientas

| Necesidad | Herramienta | Uso |
|---|---|---|
| ORM | Spring Data JPA + Hibernate ORM | Entidades, relaciones, *dirty checking*, bloqueos |
| Restricciones en las entidades | Bean Validation (Hibernate Validator) | Se validan automáticamente antes de cada `INSERT`/`UPDATE`; `@NotNull`, `@Size` y `@Min`/`@Max` también se reflejan en el esquema generado |
| Consultas tipadas | Specifications (Criteria API) + metamodelo estático (`hibernate-jpamodelgen`) | Filtros del catálogo sin cadenas SQL: `Producto_.estado` |
| Reportes | JPQL con proyecciones a `record` | Consultas sobre entidades, no sobre tablas |
| Versionado del esquema | Liquibase con changelogs **YAML** generados por *diff* desde las entidades (extensión `liquibase-hibernate`) | `spring.jpa.hibernate.ddl-auto=validate`: Hibernate verifica al arrancar que las entidades coinciden con la BD |

Configuración base (`application.yml`):

```yaml
spring:
  jpa:
    open-in-view: false            # las consultas ocurren en la capa de negocio, no al serializar
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc.time_zone: UTC
        default_batch_fetch_size: 50
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

Flujo para cambiar el esquema: **modificar la entidad → `mvn liquibase:diff` genera el changeset YAML → revisarlo → commit**. Nunca se edita un changeset ya aplicado.

> **RNF10 exige un `CHECK (stock_actual >= 0)` en la base de datos.** Se declara en la entidad con `@Check` de Hibernate (sección 5.3). Al revisar cada changeset generado, confirmar que la restricción quedó incluida; si el *diff* de Liquibase Community no la detecta, se agrega en un changeset de una sola línea. Es la única excepción documentada a la regla de no escribir SQL.

### 5.2 Mapeo del modelo relacional a entidades

| Tabla(s) | Entidad | Mapeo clave |
|---|---|---|
| `usuario`, `cliente`, `administrador` | `Usuario` (abstracta), `Cliente`, `Administrador` | `@Inheritance(strategy = JOINED)` + `@DiscriminatorColumn("tipo_usuario")`: tablas especializadas que comparten la PK, igual que el diseño |
| `categoria` | `Categoria` | `@ManyToOne padre` + `@OneToMany hijos` (relación reflexiva) |
| `producto_material` | `ProductoMaterial` | Clase asociación con `@EmbeddedId` + `@MapsId` |
| `variante_producto` | `VarianteProducto` | Métodos `reservar`, `liberar`, `confirmarSalida`, `ajustar` que protegen sus invariantes |
| `pedido` / `detalle_pedido` | `Pedido` / `DetallePedido` | Composición: `@OneToMany(cascade = ALL, orphanRemoval = true)` |
| `carrito` / `item_carrito` | `Carrito` / `ItemCarrito` | Composición |
| `producto` / `imagen_producto` | `Producto` / `ImagenProducto` | Composición |
| `metadato_seo` | `MetadatoSeo` | `datosEstructurados` como `Map<String,Object>` con `@JdbcTypeCode(SqlTypes.JSON)` → `jsonb` |
| `pago`, `envio`, `promocion`, `resena`, `movimiento_inventario` | Entidad homónima | `@ManyToOne(fetch = LAZY)` hacia su dueño |
| `historial_estado_pedido`, `auditoria`, `evento_pasarela`, `refresh_token` | Entidad homónima | Trazabilidad, idempotencia y refresh tokens JWT (secciones 8 y 9) |

Convenciones de mapeo:
- Claves: `Long` con `@GeneratedValue(strategy = IDENTITY)` y `@Column(name = "id_<tabla>")`.
- Nombres de columna: la estrategia por defecto de Spring Boot convierte `pesoGramos` → `peso_gramos`, así que solo se anota lo que difiere.
- Estados: `enum` con `@Enumerated(EnumType.STRING)` (los enums persistidos viven en `datos.entidad`).
- Dinero: `BigDecimal` con `@Column(precision = 12, scale = 2)`.
- Fechas: `OffsetDateTime` con `@CreationTimestamp` / `@UpdateTimestamp`.
- Todas las relaciones `LAZY`; para evitar N+1 se usan `@EntityGraph` en los repositorios.
- `@Version` en `VarianteProducto`, `Pedido` y `Promocion` (bloqueo optimista para ediciones del panel).
- Producto con pedidos asociados no se borra: cambia a `ARCHIVADO` (agregación, sin cascada).

### 5.3 Entidades de ejemplo

**Herencia de usuarios**

```java
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @NotBlank @Size(max = 60)  private String nombre;
    @NotBlank @Size(max = 60)  private String apellido;

    @NotBlank @Email @Size(max = 120)
    @Column(unique = true)
    private String correo;

    @NotBlank @Size(max = 255) private String contrasenaHash;
    @Size(max = 20)            private String telefono;

    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @CreationTimestamp private OffsetDateTime fechaRegistro;
}

@Entity
@Table(name = "cliente")
@DiscriminatorValue("CLIENTE")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Cliente extends Usuario {

    @Size(max = 20) private String documento;
    private boolean aceptaComunicaciones;
    @NotNull private OffsetDateTime politicaAceptadaEn;   // Ley 1581 (RNF05)

    @OneToMany(mappedBy = "cliente")
    private List<Direccion> direcciones = new ArrayList<>();
}
```

**Variante con sus invariantes de inventario**

```java
@Entity
@Table(name = "variante_producto")
@Check(constraints = "stock_actual >= 0 AND stock_reservado >= 0 AND stock_reservado <= stock_actual") // RNF10
public class VarianteProducto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @NotBlank @Size(max = 40) @Column(unique = true)
    private String sku;

    @Size(max = 20) private String talla;

    @Column(precision = 8, scale = 2)
    private BigDecimal pesoGramos;

    @NotNull @DecimalMin("0.00") @Column(precision = 12, scale = 2)
    private BigDecimal precioAdicional = BigDecimal.ZERO;

    @Min(0) private int stockActual;      // RNF10: nunca negativo
    @Min(0) private int stockReservado;

    private boolean activa = true;

    @Version private long version;

    // Restricción entre campos: se valida antes de cada INSERT/UPDATE
    @AssertTrue(message = "Las unidades reservadas no pueden superar la existencia")
    private boolean isReservaConsistente() { return stockReservado <= stockActual; }

    public int disponible() { return stockActual - stockReservado; }

    public void reservar(int cantidad) {
        if (cantidad <= 0 || cantidad > disponible())
            throw new IllegalStateException("Existencias insuficientes para " + sku);
        stockReservado += cantidad;
    }

    public void liberar(int cantidad)         { stockReservado -= Math.min(cantidad, stockReservado); }
    public void confirmarSalida(int cantidad) { stockReservado -= cantidad; stockActual -= cantidad; }

    public void ajustar(int delta) {
        if (stockActual + delta < stockReservado)
            throw new IllegalStateException("El ajuste dejaría reservas sin respaldo");
        stockActual += delta;
    }
}
```

> Las entidades lanzan `IllegalStateException` como última barrera; la capa de negocio valida antes y lanza sus propias excepciones (la capa de datos no depende de `negocio`, ver 4.2).

**Clase asociación Producto–Material**

```java
@Embeddable
public record ProductoMaterialId(Long idProducto, Long idMaterial) implements Serializable {}

@Entity
@Table(name = "producto_material")
public class ProductoMaterial {

    @EmbeddedId private ProductoMaterialId id;

    @MapsId("idProducto") @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @MapsId("idMaterial") @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material")
    private Material material;

    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false)
    private RolMaterial rol;                       // METAL_PRINCIPAL, PIEDRA, SECUNDARIO

    @Column(precision = 8, scale = 2) private BigDecimal pesoGramos;
    @Column(precision = 6, scale = 2) private BigDecimal quilates;
}
```

**Pedido como agregado (composición)**

```java
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @NotBlank @Size(max = 20) @Column(unique = true)
    private String numeroPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_usuario")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_direccion")
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_promocion")
    private Promocion promocion;

    @Enumerated(EnumType.STRING) @Column(length = 25, nullable = false)
    private EstadoPedido estado = EstadoPedido.PENDIENTE_PAGO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<Pago> pagos = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Envio envio;

    @NotNull @DecimalMin("0.00") @Column(precision = 12, scale = 2) private BigDecimal subtotal;
    @NotNull @DecimalMin("0.00") @Column(precision = 12, scale = 2) private BigDecimal descuento = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(precision = 12, scale = 2) private BigDecimal costoEnvio = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(precision = 12, scale = 2) private BigDecimal total;

    private boolean reservaActiva = true;
    private OffsetDateTime reservaExpiraEn;

    @CreationTimestamp private OffsetDateTime fecha;
    @Version private long version;

    @AssertTrue(message = "El total no coincide con subtotal − descuento + envío")
    private boolean isTotalConsistente() {
        return total == null || total.compareTo(subtotal.subtract(descuento).add(costoEnvio)) == 0;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalle.asignarPedido(this);
        detalles.add(detalle);
    }
}
```

**Metadatos SEO con JSON**

```java
@Entity
@Table(name = "metadato_seo")
public class MetadatoSeo {
    // ...
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_producto", unique = true)
    private Producto producto;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_categoria", unique = true)
    private Categoria categoria;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> datosEstructurados;   // JSON-LD Schema.org Product/Offer

    @AssertTrue(message = "El metadato debe pertenecer a un producto o a una categoría, no a ambos")
    private boolean isDuenoUnico() { return (producto == null) != (categoria == null); }
}
```

### 5.4 Repositorios y consultas sin SQL

**Consultas derivadas**

```java
public interface ProductoRepository extends JpaRepository<Producto, Long>,
                                            JpaSpecificationExecutor<Producto> {
    @EntityGraph(attributePaths = {"variantes", "imagenes", "materiales.material"})
    Optional<Producto> findBySlugAndEstado(String slug, EstadoProducto estado);

    boolean existsBySku(String sku);
}
```

**Bloqueo pesimista para la reserva (RNF11)** — Hibernate genera `SELECT … FOR UPDATE`:

```java
public interface VarianteProductoRepository extends JpaRepository<VarianteProducto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    List<VarianteProducto> findByIdInOrderByIdAsc(Collection<Long> ids);   // orden fijo = sin deadlocks
}

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pedido> findByNumeroPedido(String numeroPedido);              // webhook de pago

    List<Pedido> findByReservaActivaTrueAndReservaExpiraEnBefore(OffsetDateTime ahora);
}
```

**Filtros del catálogo con Specifications (RF06)**

```java
public final class ProductoSpecs {

    public static Specification<Producto> publicado() {
        return (r, q, cb) -> cb.equal(r.get(Producto_.estado), EstadoProducto.PUBLICADO);
    }

    public static Specification<Producto> deCategoria(String slug) {
        return (r, q, cb) -> slug == null ? null
             : cb.equal(r.get(Producto_.categoria).get(Categoria_.slug), slug);
    }

    public static Specification<Producto> conMaterial(Long idMaterial) {
        return (r, q, cb) -> {
            if (idMaterial == null) return null;
            q.distinct(true);
            var pm = r.join(Producto_.materiales);
            return cb.equal(pm.get(ProductoMaterial_.material).get(Material_.id), idMaterial);
        };
    }

    public static Specification<Producto> textoContiene(String texto) {
        return (r, q, cb) -> texto == null ? null
             : cb.like(cb.lower(r.get(Producto_.nombre)), "%" + texto.toLowerCase() + "%");
    }
}

// En negocio.catalogo.CatalogoService:
var spec = Specification.where(publicado())
        .and(deCategoria(filtro.categoria()))
        .and(conMaterial(filtro.material()))
        .and(textoContiene(filtro.q()));
productoRepository.findAll(spec, pageable);
```

**Reportes con JPQL y proyecciones (RF15)**

```java
public record VentaPorMaterial(String material, Long unidades, BigDecimal valor) {}

@Query("""
    select new co.nalu.datos.repositorio.proyeccion.VentaPorMaterial(
           m.tipo, sum(d.cantidad), sum(d.precioUnitario * d.cantidad))
    from DetallePedido d
      join d.variante v join v.producto p join p.materiales pm join pm.material m
    where d.pedido.estado in :estados
      and d.pedido.fecha between :desde and :hasta
      and pm.rol = :rol
    group by m.tipo
    """)
List<VentaPorMaterial> ventasPorMaterial(Collection<EstadoPedido> estados,
                                         OffsetDateTime desde, OffsetDateTime hasta,
                                         RolMaterial rol);
```

**Prohibido:** `@Query(nativeQuery = true)`, `JdbcTemplate`, `NamedParameterJdbcTemplate` y `EntityManager.createNativeQuery`. Lo verifica ArchUnit (sección 4.4).

### 5.5 Restricciones que se garantizan en la capa de negocio

Algunas reglas no se pueden expresar con anotaciones JPA sin escribir SQL. Se garantizan con la combinación de invariantes de entidad, transacción y bloqueo del registro padre:

| Regla | Cómo se garantiza |
|---|---|
| Una sola imagen principal por producto | `ProductoService` desmarca las demás en la misma transacción |
| Un solo carrito activo por cliente | `CarritoService` busca el activo antes de crear y fusiona al iniciar sesión |
| Un solo pago aprobado por pedido | El webhook bloquea el pedido (`findByNumeroPedido` con `PESSIMISTIC_WRITE`) y solo aprueba si sigue en `PENDIENTE_PAGO` |
| `stock_reservado ≤ stock_actual`, total consistente, metadato con dueño único, fechas de promoción | `@AssertTrue` en la entidad (validado antes de cada escritura) |
| Webhook procesado una sola vez | `EventoPasarela.claveIdempotencia` con `@Column(unique = true)` |

### 5.6 Datos semilla

`negocio.cuentas.DatosSemillaRunner` (`ApplicationRunner`, perfiles `dev` y `demo`) crea con los repositorios, solo si no existen:

- **Materiales:** Plata 925, Acero 316L, Oro 18K, Esmeralda, Cuarzo (`factorPrecio = 1` hasta definir la fórmula).
- **Categorías:** Anillos, Collares, Aretes, Pulseras.
- **Administrador:** a partir de `NALU_ADMIN_EMAIL` y `NALU_ADMIN_PASSWORD` (nunca contraseñas en el repositorio).

---

## 6. Reglas de negocio críticas

### 6.1 Precio de venta
Propuesta (confirmar, ver sección 10):

```
precio_venta(variante) = producto.precio_base × factor_precio(material METAL_PRINCIPAL)
                         + variante.precio_adicional
```
Redondeo a pesos enteros (`RoundingMode.HALF_UP`). El precio se calcula en el backend; el frontend nunca envía precios.

### 6.2 Disponibilidad
```
disponible(variante) = stock_actual − stock_reservado
```
Un producto aparece como "agotado" si todas sus variantes activas tienen disponibilidad 0.

### 6.3 Checkout y reserva (RF09, RF10, RNF10, RNF11)
Todo en **un solo método `@Transactional`** en `negocio.pedidos.CheckoutService` (capa de negocio); los bloqueos los ejecuta el repositorio en la capa de datos:

1. Cargar las variantes del carrito con bloqueo de fila, **ordenadas por id** (evita *deadlocks*):
   `VarianteProductoRepository.findByIdInOrderByIdAsc` con `@Lock(PESSIMISTIC_WRITE)`; Hibernate genera el `SELECT … FOR UPDATE`.
2. Verificar `disponible >= cantidad` para cada ítem; si no, abortar con 409 y detalle por variante.
3. Validar cupón (vigencia, `activa`, `usos_actuales < usos_maximos`) y calcular descuento.
4. Calcular subtotal, costo de envío y total con precios recalculados en el servidor.
5. `variante.reservar(cantidad)` en cada variante (método de la entidad).
6. Persistir con `pedidoRepository.save(...)` el `Pedido` (`PENDIENTE_PAGO`, `reserva_activa = true`, `reserva_expira_en = now() + TTL`), `detalle_pedido`, `historial_estado_pedido` y `pago` (`INICIADO`).
7. Marcar el carrito como `CONVERTIDO`.

Si cualquier paso falla, se revierte todo. Hibernate sincroniza los cambios de las entidades al confirmar la transacción (*dirty checking*), sin `UPDATE` escritos a mano.

### 6.4 Confirmación de pago por webhook (RF12, RNF03)
`POST /api/v1/webhooks/wompi` — público, sin sesión, protegido por firma. `api.webhook.WompiWebhookController` solo recibe el cuerpo y delega; la verificación de firma la hace `integracion.wompi` y el procesamiento `negocio.pagos.PagoService`:

1. Verificar la firma del evento (checksum SHA-256 con el *events secret*, según la documentación de eventos de Wompi). Firma inválida → 401 y no se modifica nada.
2. Insertar en `evento_pasarela` con `clave_idempotencia = idTransaccion:estado`. Si viola el `UNIQUE` → ya procesado → responder 200 sin hacer nada.
3. Buscar el `pago` por `referencia`, bloquear el pedido (`FOR UPDATE`) y, según el estado:
   - **APPROVED** y pedido en `PENDIENTE_PAGO`: validar que el monto coincida; `stock_actual -= cant` y `stock_reservado -= cant`; registrar `movimiento_inventario` tipo `SALIDA` con `id_pedido`; pedido → `PAGADO`; pago → `APROBADO`; `usos_actuales++` del cupón; `reserva_activa = false`.
   - **DECLINED / VOIDED / ERROR**: liberar la reserva (`stock_reservado -= cant`), pago → `RECHAZADO`/`ANULADO`/`ERROR`, pedido sigue en `PENDIENTE_PAGO` (el cliente puede reintentar).
   - **PENDING**: pago → `PENDIENTE`, no se toca el inventario.
4. Enviar notificación **después del commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`).
5. Responder 200 siempre que el evento sea válido (la pasarela reintenta si no recibe 200).

> Nunca confiar en la redirección del navegador para marcar un pedido como pagado; solo el webhook (o una consulta directa a la API de la pasarela) cambia el estado.

### 6.5 Expiración de reservas
Tarea `@Scheduled` cada minuto: pedidos con `reserva_activa = true AND reserva_expira_en < now()` → liberar reserva y `reserva_activa = false`. Para reintentar el pago se vuelve a validar y reservar (pasos 1–5 de 6.3).

### 6.6 Máquina de estados del pedido (RF14)

```
PENDIENTE_PAGO ──(webhook APPROVED)──► PAGADO ──► EN_PREPARACION ──(guía registrada)──► ENVIADO ──► ENTREGADO
      │                                  │               │
      └──────────────► CANCELADO ◄───────┴───────────────┘
```

| Desde | Hacia | Quién | Efecto |
|---|---|---|---|
| PENDIENTE_PAGO | PAGADO | Sistema (webhook) | Descuenta inventario |
| PENDIENTE_PAGO | CANCELADO | Admin | Libera reserva si sigue activa |
| PAGADO / EN_PREPARACION | CANCELADO | Admin (motivo obligatorio) | `ENTRADA` de reintegro al inventario |
| PAGADO | EN_PREPARACION | Admin | — |
| EN_PREPARACION | ENVIADO | Admin | Requiere registro en `envio` |
| ENVIADO | ENTREGADO | Admin | `envio.fecha_entrega` |

Implementar como `enum EstadoPedido` con método `puedeTransicionarA(EstadoPedido)`; cualquier transición no permitida → 409. Cada cambio inserta en `historial_estado_pedido`.

### 6.7 Movimientos manuales de inventario (RF13)
Bloquear la variante, validar que `stock_actual + cantidad >= stock_reservado` (no se puede retirar stock ya reservado), actualizar e insertar el movimiento con el administrador responsable.

---

## 7. API REST (`/api/v1`)

Formato JSON, paginación `?page=0&size=20&sort=campo,asc`, errores `problem+json`. Toda entrada se valida con el esquema YAVI del DTO; las rutas protegidas exigen `Authorization: Bearer <access token>`.

### Público (sin token)
| Método | Ruta | RF |
|---|---|---|
| POST | `/auth/registro` | RF01 |
| POST | `/auth/login` (devuelve access token + cookie de refresh) | RF01 |
| POST | `/auth/refresh` · `/auth/logout` (usan la cookie `nalu_rt`) | RF01, RNF01 |
| GET | `/categorias` (árbol) · `/materiales` | RF06 |
| GET | `/productos?q&categoria&material&precioMin&precioMax&disponible` | RF06 |
| GET | `/productos/{slug}` (ficha + variantes + materiales + reseñas aprobadas + SEO) | RF07 |
| GET/POST/PATCH/DELETE | `/carrito`, `/carrito/items`, `/carrito/items/{id}` (header `X-Cart-Token` para visitantes) | RF08 |
| POST | `/webhooks/wompi` | RF12 |

### Cliente autenticado (`Authorization: Bearer`)
| Método | Ruta | RF |
|---|---|---|
| GET | `/auth/me` | RF01 |
| GET/PUT | `/cuenta/perfil` · POST `/cuenta/solicitud-supresion` | RF02, RNF05 |
| CRUD | `/cuenta/direcciones` | RF02 |
| POST | `/carrito/fusionar` (al iniciar sesión) | RF08 |
| POST | `/checkout/cotizar` (valida stock, cupón y envío sin crear nada) | RF09, RF11 |
| POST | `/pedidos` (crea pedido + reserva + pago iniciado; devuelve datos para el widget) | RF10 |
| POST | `/pedidos/{numero}/reintentar-pago` | RF12 |
| GET | `/cuenta/pedidos` · `/cuenta/pedidos/{numero}` | RF14 |
| POST | `/productos/{id}/resenas` (solo si tiene un pedido ENTREGADO con el producto) | RF07 |

### Administrador (`/admin/**`, rol `ADMINISTRADOR`)
| Método | Ruta | RF |
|---|---|---|
| CRUD | `/admin/categorias` | RF03 |
| CRUD | `/admin/productos`, `/admin/productos/{id}/materiales`, `/variantes`, `/imagenes` · PATCH `/admin/productos/{id}/estado` | RF04 |
| GET/PUT | `/admin/seo/productos/{id}` · `/admin/seo/categorias/{id}` | RF05 |
| CRUD | `/admin/promociones` | RF11 |
| GET/POST | `/admin/inventario/movimientos` · GET `/admin/inventario/alertas` | RF13 |
| GET | `/admin/pedidos?estado&desde&hasta` · `/admin/pedidos/{id}` | RF14 |
| PATCH | `/admin/pedidos/{id}/estado` · POST `/admin/pedidos/{id}/envio` | RF14 |
| PATCH | `/admin/resenas/{id}` (aprobar/rechazar) | RF07 |
| GET | `/admin/reportes/{ventas|rotacion|agotados|cupones|pagos}?desde&hasta&formato=json|csv` | RF15 |

---

## 8. Seguridad con JWT (RNF01–RNF05)

La API es **sin estado** (`SessionCreationPolicy.STATELESS`) y se autentica con **JWT**. Se usa el soporte nativo de Spring Security (`spring-boot-starter-oauth2-resource-server`, que trae Nimbus JOSE + JWT) tanto para **emitir** como para **validar** los tokens; no se escribe un filtro JWT a mano.

### 8.1 Esquema de tokens

| Token | Formato | Vida | Dónde vive en el cliente | Para qué |
|---|---|---|---|---|
| **Access token** | JWT firmado HS256 | 15 min | En memoria (un `signal` del `AuthService` de Angular) — nunca en `localStorage` | Header `Authorization: Bearer …` en cada llamada a la API |
| **Refresh token** | Valor aleatorio opaco (256 bits) | Expira tras **30 min sin uso** (máximo absoluto 7 días) | Cookie `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth` | Obtener un nuevo access token |

**Cumplimiento de RNF01 (expiración por inactividad de 30 min):** cada uso del refresh token lo **rota** y extiende su vida 30 minutos; si el usuario no hace nada durante 30 minutos, el refresh token expira y debe autenticarse de nuevo. Como el access token dura 15 minutos, nunca sobrevive más allá de esa ventana.

**Claims del access token:**

```json
{
  "iss": "https://api.nalu.co",
  "sub": "42",
  "roles": ["CLIENTE"],
  "nombre": "Ana",
  "iat": 1762680000,
  "exp": 1762680900,
  "jti": "1c9f…"
}
```

Sin datos personales sensibles en el token: se puede decodificar en el navegador.

### 8.2 Flujos

```
LOGIN        POST /auth/login {correo, contrasena}
             ← 200 {accessToken, expiraEn, usuario}  + Set-Cookie: nalu_rt=…

LLAMADA      GET /cuenta/pedidos   Authorization: Bearer <access>
             ← 200 …  |  401 si expiró

RENOVACIÓN   POST /auth/refresh    (cookie nalu_rt)
             ← 200 {accessToken, expiraEn}  + Set-Cookie: nalu_rt=<nuevo>   (rotación)

RECARGA      Al abrir la app, Angular llama /auth/refresh: si la cookie sigue vigente,
DE PÁGINA    recupera la sesión sin pedir contraseña.

LOGOUT       POST /auth/logout  → revoca el refresh token y borra la cookie.
             El access token vigente expira solo (máx. 15 min).
```

### 8.3 Refresh tokens: almacenamiento, rotación y detección de robo

Entidad `RefreshToken` (tabla `refresh_token`):

| Campo | Descripción |
|---|---|
| `id`, `usuario` | Dueño del token |
| `hashToken` (único) | **SHA-256** del valor; el valor en claro nunca se guarda |
| `familia` (UUID) | Todos los tokens surgidos del mismo login comparten familia |
| `creadoEn`, `expiraEn`, `limiteAbsoluto` | `expiraEn = creadoEn + 30 min`, sin pasar de `limiteAbsoluto`. `limiteAbsoluto = login + 7 días` se fija al iniciar sesión y lo hereda cada token rotado (si se calculara desde `creadoEn`, se reiniciaría en cada refresh). Se descartó `ultimoUso`: con rotación en cada uso coincide siempre con `creadoEn` |
| `revocadoEn`, `reemplazadoPor` | Trazabilidad de la rotación |
| `ip`, `userAgent` | Auditoría (RNF15) |

Reglas en `negocio.cuentas.TokenService`:
1. **Rotación:** cada `/auth/refresh` revoca el token presentado y emite uno nuevo de la misma familia.
2. **Detección de reutilización:** si llega un refresh token **ya revocado**, se asume robo y se **revoca toda la familia** (el atacante y la víctima deben volver a iniciar sesión).
3. **Cambio de contraseña o bloqueo de cuenta:** revoca todas las familias del usuario.
4. Tarea `@Scheduled` diaria que elimina tokens expirados hace más de 30 días.

### 8.4 Configuración de Spring Security

```java
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())            // API sin cookies de sesión; ver 8.5 para /auth/refresh
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/webhooks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**",
                                 "/api/v1/materiales/**").permitAll()
                .requestMatchers("/api/v1/carrito/**").permitAll()      // visitante con X-Cart-Token
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRoles())))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())   // 401
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))            // 403
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        var decoder = NimbusJwtDecoder.withSecretKey(props.claveSecreta())
                                      .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.emisor()));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(props.claveSecreta()));
    }

    private JwtAuthenticationConverter convertidorRoles() {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(roles);
        return conv;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}
```

Emisión del access token (`negocio.cuentas.TokenService`):

```java
public String emitirAccessToken(Usuario u) {
    var ahora = Instant.now();
    var claims = JwtClaimsSet.builder()
        .issuer(props.emisor())
        .subject(u.getId().toString())
        .issuedAt(ahora)
        .expiresAt(ahora.plus(props.vidaAccessToken()))      // 15 min
        .id(UUID.randomUUID().toString())
        .claim("roles", List.of(u instanceof Administrador ? "ADMINISTRADOR" : "CLIENTE"))
        .claim("nombre", u.getNombre())
        .build();
    var cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
}
```

> `JwtEncoder`/`JwtDecoder` son beans de configuración; la capa de negocio los usa a través de esas interfaces de Spring, por lo que no viola la regla de capas.

### 8.5 Resto de controles

- **Clave de firma:** `JWT_SECRET` de al menos 256 bits en Base64, solo en variables de entorno o en el gestor de secretos del despliegue. Si en el futuro otro servicio debe validar los tokens, migrar a RS256 con par de llaves.
- **Protección del endpoint de refresh:** al usar cookie, `/auth/refresh` y `/auth/logout` exigen `SameSite=Strict` y verifican que el header `Origin` coincida con `APP_FRONTEND_URL`. El resto de la API usa solo el header `Authorization`, por lo que no es vulnerable a CSRF.
- **CORS:** solo el origen de `APP_FRONTEND_URL`, con `allowCredentials(true)` para la cookie del refresh.
- **Autorización por dueño:** en los servicios de cliente, toda consulta filtra por el `sub` del token (`@AuthenticationPrincipal Jwt jwt`); un pedido ajeno responde **404** (RNF02).
- **Contraseñas:** `BCryptPasswordEncoder` (factor 12), mínimo 8 caracteres validado con YAVI (RNF01).
- **Fuerza bruta:** tras 5 intentos fallidos de login por correo en 15 minutos, bloqueo temporal de 15 minutos (registro de intentos en BD o Bucket4j). *Pendiente: no se implementó en F0-11; se hará en F1 junto con el registro de clientes.*
- **Webhook:** `permitAll`, protegido por la firma de Wompi (sección 6.4).
- **Datos de pago:** nunca llegan al backend; el pago se completa en el widget de la pasarela (RNF04).
- **Ley 1581 (RNF05):** `politicaAceptadaEn` obligatorio en el registro y endpoint de solicitud de supresión.

### 8.6 Frontend (Angular)

| Pieza | Responsabilidad |
|---|---|
| `core/auth/auth.service.ts` | Guarda el access token y el usuario en `signal`s; `login`, `logout`, `refrescar()`; al iniciar la app intenta `refrescar()` |
| `core/auth/auth.interceptor.ts` | Añade `Authorization: Bearer` solo a las URL de la API; ante un 401 hace **un único** refresh compartido entre solicitudes concurrentes y reintenta; si falla, cierra sesión y redirige a `/cuenta/login` |
| `core/auth/rol.guard.ts` | Protege `/admin/**` y `/cuenta/**` según el claim `roles` (solo para la UI; quien decide es el backend) |
| `withCredentials: true` | Solo en las llamadas a `/auth/refresh` y `/auth/logout`, para enviar la cookie |

Las rutas renderizadas en el servidor (catálogo y fichas) son públicas y no usan token; las rutas con sesión se renderizan en el cliente (sección 11).

---

## 9. Ajustes técnicos respecto al diseño

Cambios menores al modelo relacional que surgen al implementar (se crean como atributos o entidades JPA); conviene reflejarlos en la siguiente versión del documento:

| Ajuste | Motivo |
|---|---|
| `cliente.politica_aceptada_en` | Evidencia de la autorización de tratamiento de datos (Ley 1581, RNF05). |
| `direccion.activa` | Una dirección usada en pedidos no puede borrarse; se desactiva. |
| `promocion.usos_actuales` | Necesario para validar `usos_maximos` (RF11). |
| `pedido.reserva_activa`, `pedido.reserva_expira_en` | Saber qué reservas liberar cuando el pago expira (RF09). |
| `pago.referencia` | Referencia única que se envía a la pasarela; permite varios intentos por pedido. |
| `movimiento_inventario.id_pedido` y `id_usuario` nullable | Las salidas por venta las registra el sistema, no un administrador. |
| `historial_estado_pedido`, `auditoria` | Trazabilidad exigida por RNF15. |
| `evento_pasarela` | Idempotencia del webhook (marco conceptual 11.2). |
| `MetadatoSeo.isDuenoUnico()` (`@AssertTrue`) | Garantiza que cada registro pertenece a un producto **o** a una categoría. |
| `refresh_token` | Refresh tokens JWT con rotación y detección de reutilización (sección 8.3). |

---

## 10. Decisiones pendientes

- [ ] **Fórmula de precio** por material (¿`factor_precio` multiplica el precio base? ¿influye el peso en gramos del oro?).
- [ ] **Costo de envío** en el MVP: tarifa plana configurable, tarifa por departamento o envío gratis desde un monto.
- [ ] **TTL de la reserva**: 30 min sugerido; PSE puede tardar más, evaluar 60 min.
- [ ] ¿Cancelación automática de pedidos `PENDIENTE_PAGO` después de N horas?
- [ ] Almacenamiento de imágenes: disco local en dev; en despliegue, Google Cloud Storage u otro bucket.
- [ ] Hosting del prototipo (p. ej., Cloud Run + Cloud SQL) y dominio.
- [ ] Formato de `numero_pedido` (sugerido: `NALU-AAAA-000123`).

---

## 11. Frontend (Angular)

Una sola aplicación con rutas *lazy*; la tienda pública se renderiza en el servidor y el panel/cuenta en el cliente.

```
frontend/src/app/
├── core/          # interceptores (errores, XSRF), guards (auth, admin), servicios de API, modelos
├── shared/        # componentes UI reutilizables (tarjeta de producto, paginador, alertas, etc.)
├── tienda/        # inicio, catálogo, categoría, ficha, carrito, checkout, resultado de pago
├── cuenta/        # login, registro, perfil, direcciones, mis pedidos
├── admin/         # dashboard, productos, inventario, pedidos, promociones, contenido, reseñas, reportes
├── app.routes.ts
└── app.routes.server.ts
```

`app.routes.server.ts` (modos de renderizado):
- `RenderMode.Server` → `/`, `/catalogo/**`, `/producto/:slug` (SEO, RNF07).
- `RenderMode.Client` → `/cuenta/**`, `/checkout/**`, `/admin/**`.

SEO en la ficha de producto: `Title` y `Meta` de Angular + `<link rel="canonical">` + `<script type="application/ld+json">` con `Product` y `Offer` (`price`, `priceCurrency: "COP"`, `availability`), generado desde `metadato_seo.datos_estructurados`.

Accesibilidad (RNF09): `alt` obligatorio en imágenes, formularios con `<label>`, foco visible, contraste AA; auditar con Lighthouse/axe en cada fase.

---

## 12. Entorno local

`docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: nalu
      POSTGRES_USER: nalu
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
  mailpit:
    image: axllent/mailpit
    ports: ["8025:8025", "1025:1025"]   # UI en http://localhost:8025
volumes:
  pgdata:
```

`.env.example`:

```
DB_URL=jdbc:postgresql://localhost:5432/nalu
DB_USER=nalu
DB_PASSWORD=cambiar
NALU_ADMIN_EMAIL=admin@nalu.local
NALU_ADMIN_PASSWORD=cambiar
WOMPI_BASE_URL=https://sandbox.wompi.co/v1
WOMPI_PUBLIC_KEY=pub_test_xxx
WOMPI_PRIVATE_KEY=prv_test_xxx
WOMPI_INTEGRITY_SECRET=test_integrity_xxx
WOMPI_EVENTS_SECRET=test_events_xxx
MAIL_HOST=localhost
MAIL_PORT=1025
APP_FRONTEND_URL=http://localhost:4200
RESERVA_TTL_MINUTOS=30
JWT_SECRET=generar_con_openssl_rand_base64_32
JWT_ISSUER=http://localhost:8080
JWT_ACCESS_TTL=15m
JWT_REFRESH_INACTIVIDAD=30m
JWT_REFRESH_MAXIMO=7d
```

Generar la clave de firma: `openssl rand -base64 32`.

Arranque:

```bash
docker compose up -d
cd backend  && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm start
```

> Para recibir webhooks de Wompi en local se necesita un túnel público (p. ej., `cloudflared tunnel` o `ngrok`) y registrar esa URL de eventos en el panel de sandbox de Wompi.

---

## 13. Plan por fases

| Fase | Objetivo | Requerimientos | Entregable verificable |
|---|---|---|---|
| **F0 — Fundaciones** | Repo, Docker Compose, entidades JPA + primer changeset Liquibase, YAVI con mensajes en español, seguridad JWT (login/refresh/logout), Angular con SSR e interceptor, CI en GitHub Actions | RNF01, RNF13 | `docker compose up` + backend y frontend arrancan; CI en verde |
| **F1 — Cuentas y catálogo (admin)** | Registro/login, perfil, direcciones, CRUD de categorías, productos, materiales, variantes e imágenes | RF01–RF04, RNF01, RNF02, RNF05 | El administrador publica una pieza con 2 materiales y 3 tallas |
| **F2 — Tienda pública y SEO** | Listado con filtros, ficha de producto, metadatos y JSON-LD con SSR | RF05–RF07, RNF07, RNF09 | Ficha pasa el validador de Schema.org; Lighthouse sin errores críticos |
| **F3 — Carrito y checkout** | Carrito visitante/cliente, fusión, cotización, cupones, creación del pedido con reserva | RF08–RF11, RNF10, RNF11 | Prueba concurrente: dos compras de la última unidad → solo una se confirma |
| **F4 — Pagos** | Integración Wompi sandbox, webhook firmado e idempotente, expiración de reservas, notificaciones | RF12, RNF03, RNF04 | Pago aprobado, rechazado y evento duplicado se comportan según 6.4 |
| **F5 — Operación del negocio** | Movimientos de inventario, gestión de pedidos y envíos, historial, reseñas, reportes + CSV | RF13–RF15, RNF15 | Pedido recorre todos los estados; reporte de ventas por material |
| **F6 — Calidad y cierre** | Cobertura ≥ 60 %, pruebas de carga (50 usuarios), compatibilidad de navegadores, respaldo y restauración de BD | RNF06, RNF08, RNF12–RNF14 | Informe de verificación de RNF |

---

## 14. Pruebas obligatorias

- **Unitarias (JUnit + Mockito):** cálculo de precio, cálculo de descuento, máquina de estados, validación de cupones, invariantes de `VarianteProducto`.
- **Esquemas YAVI:** cada `ESQUEMA` con casos válidos e inválidos (incluidas las reglas entre campos) y verificación de que los mensajes salen en español.
- **Integración (Testcontainers + PostgreSQL real):**
  - Checkout atómico: fallo simulado a mitad del proceso → no queda pedido sin detalle ni inventario alterado (RNF10).
  - Concurrencia: dos hilos compran la última unidad → uno recibe 409 (RNF11).
  - Webhook: firma alterada → 401; evento repetido → un solo descuento de inventario.
  - Control de acceso: todas las rutas `/admin/**` sin token → 401 y con rol cliente → 403 (RNF02).
  - JWT: token expirado o con firma alterada → 401; `/auth/refresh` rota el token; reutilizar un refresh token revocado revoca toda la familia; tras 30 min sin uso el refresh es rechazado (RNF01).
  - Esquema: Hibernate arranca con `ddl-auto=validate` contra el esquema creado por Liquibase en Testcontainers.
- **Arquitectura (ArchUnit):** `ArquitecturaCapasTest` (sección 4.4) verifica en cada build que se respeten las capas y que no haya SQL nativo.
- **Cobertura:** JaCoCo con umbral ≥ 60 % en `pedidos`, `inventario` y `promociones` (falla el build si no se cumple).

---

## 15. Convenciones de trabajo

- **Ramas:** `main` (estable) + `feat/<modulo>-<descripcion>`, `fix/...`; PR hacia `main` con CI en verde.
- **Commits:** Conventional Commits en español — `feat(pedidos): reserva de existencias en checkout`.
- **Esquema:** se cambia la entidad y se genera un changeset Liquibase nuevo; nunca editar un changeset ya aplicado.
- **Definición de terminado:** compila, pruebas pasan, endpoint documentado (OpenAPI con `springdoc-openapi`), RF trazado en el PR, sin secretos en el código.
- **ADR:** toda decisión de la sección 10 que se cierre se registra en `docs/decisiones/NNN-titulo.md`.

---

## 16. Checklist de arranque

- [ ] Crear repositorio `nalu-ecommerce` en GitHub (`mariocodem`).
- [ ] Generar backend en start.spring.io: Web, Data JPA, Security, OAuth2 Resource Server, Validation (para las entidades), PostgreSQL Driver, Liquibase, Mail, Actuator, Testcontainers; añadir YAVI (`am.ik.yavi:yavi`), `hibernate-jpamodelgen` y ArchUnit.
- [ ] Crear los paquetes de capas `api`, `negocio`, `datos`, `integracion`, `config` y el `ArquitecturaCapasTest` antes de escribir el primer módulo.
- [ ] Generar frontend: `ng new frontend --ssr --style=scss --routing`.
- [ ] Añadir `docker-compose.yml`, `.env.example`, `.gitignore`.
- [ ] Crear las entidades de la sección 5 y generar el primer changeset Liquibase; confirmar que incluye el `CHECK` de stock.
- [ ] Generar `JWT_SECRET` y configurar `SeguridadConfig` (sección 8.4).
- [ ] Configurar CI (build + test de backend y frontend).
- [ ] Crear cuenta sandbox en Wompi y obtener llaves de prueba.
- [ ] Cerrar al menos la fórmula de precio y el costo de envío antes de F3.
- [ ] Copiar el PDF de análisis y diseño en `docs/diseno/`.

---

## 17. Trazabilidad rápida RF → módulo → fase

| RF | Controlador (`api`) | Servicio (`negocio`) | Fase |
|---|---|---|---|
| RF01 | publico | cuentas | F1 |
| RF02 | cliente | clientes | F1 |
| RF03 | admin | catalogo | F1 |
| RF04 | admin | catalogo | F1 |
| RF05 | admin | contenido | F2 |
| RF06 | publico | catalogo | F2 |
| RF07 | publico, cliente, admin | catalogo, contenido | F2 / F5 (reseñas) |
| RF08 | publico, cliente | carrito | F3 |
| RF09 | cliente | inventario, pedidos | F3 |
| RF10 | cliente | pedidos | F3 |
| RF11 | cliente, admin | promociones | F3 |
| RF12 | cliente, webhook | pagos (+ `integracion.wompi`) | F4 |
| RF13 | admin | inventario | F5 |
| RF14 | cliente, admin | pedidos, envios, notificaciones (+ `integracion.correo`) | F5 |
| RF15 | admin | reportes | F5 |
