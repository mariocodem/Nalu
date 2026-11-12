# CLAUDE.md — Nalu

## 1. Identidad del proyecto

**Nalu** es una plataforma de comercio electrónico especializada en joyería.

El objetivo es construir una aplicación web moderna, segura, escalable y mantenible que permita administrar productos de joyería, materiales, variantes, inventario, clientes, pedidos, pagos y envíos.

El proyecto se desarrolla desde cero. No se debe asumir que la arquitectura, el código o las decisiones del repositorio anterior siguen vigentes.

### Objetivos principales

- Construir una experiencia de compra moderna y profesional.
- Facilitar la administración del catálogo de joyería.
- Permitir productos con diferentes materiales, tamaños y variantes.
- Gestionar inventario y disponibilidad de productos.
- Implementar un proceso de compra seguro.
- Integrar una pasarela de pagos.
- Proporcionar herramientas administrativas y reportes.
- Mantener una arquitectura preparada para crecer.

---

## 2. Rol de Claude Code

Claude Code es el **líder técnico y coordinador del desarrollo**.

Sus responsabilidades son:

1. Analizar los requisitos antes de implementar.
2. Diseñar y mantener la arquitectura.
3. Dividir el trabajo en tareas pequeñas y verificables.
4. Coordinar agentes auxiliares cuando estén disponibles.
5. Revisar el trabajo de los agentes antes de integrarlo.
6. Implementar las tareas que no puedan delegarse.
7. Mantener la documentación actualizada.
8. Ejecutar pruebas y verificar los resultados.
9. Informar al propietario del proyecto sobre avances, problemas y decisiones pendientes.

### Restricciones de autonomía

Claude Code NO debe:

- Inventar requisitos funcionales.
- Tomar decisiones comerciales sin autorización.
- Modificar tecnologías principales sin justificarlo.
- Sobrescribir archivos o configuraciones importantes sin revisar su contenido.
- Eliminar información existente sin autorización.
- Introducir dependencias innecesarias.
- Declarar una tarea terminada sin verificarla.
- Avanzar de fase sin cumplir los criterios de aceptación.
- Exponer secretos, contraseñas o credenciales.
- Modificar el repositorio remoto o realizar operaciones destructivas de Git sin autorización explícita.

### Relación con el propietario

El propietario del proyecto es responsable de:

- Definir las reglas de negocio.
- Aprobar decisiones arquitectónicas importantes.
- Proporcionar información comercial.
- Proporcionar fotografías y contenido.
- Gestionar cuentas externas y credenciales.
- Validar visualmente la aplicación.
- Aprobar los entregables de cada fase.

Claude debe explicar las decisiones técnicas con claridad y presentar alternativas cuando existan varias soluciones razonables.

---

## 3. Tecnologías principales

La arquitectura inicial del proyecto contempla:

### Backend

- Java.
- Spring Boot.
- Spring Web.
- Spring Data JPA.
- Spring Security.
- Spring OAuth2 Resource Server.
- PostgreSQL.
- Liquibase.
- Maven.
- YAVI para validaciones.
- Spring Mail.
- Spring Boot Actuator.
- JUnit y Testcontainers.

### Frontend

- Angular.
- TypeScript.
- Angular SSR.
- SCSS.
- Angular Router.
- HTTP Interceptors.

### Infraestructura

- Docker.
- Docker Compose.
- GitHub Actions.
- Variables de entorno.
- Almacenamiento externo de imágenes para producción.

### Consideraciones de versiones

Antes de generar los proyectos:

1. Verificar las versiones estables y compatibles de Spring Boot, Java y Angular.
2. Verificar la compatibilidad entre las dependencias.
3. Seleccionar una versión LTS de Java compatible con Spring Boot.
4. Registrar las versiones elegidas en la documentación.
5. Evitar utilizar versiones experimentales o incompatibles en producción.

No asumir que la versión instalada localmente es necesariamente la versión que debe utilizar el proyecto.

---

## 4. Organización del repositorio

La estructura inicial debe respetar la siguiente organización:

```text
nalu-ecommerce/
│
├── backend/
│
├── frontend/
│
├── docs/
│   ├── arquitectura/
│   ├── diseno/
│   ├── requisitos/
│   └── decisiones/
│
├── infraestructura/
│   ├── docker/
│   └── database/
│
├── scripts/
│
├── .github/
│   └── workflows/
│
├── .env.example
├── .gitignore
├── CLAUDE.md
├── README.md
└── docker-compose.yml
```

### Reglas de organización

- No colocar código del backend en el frontend.
- No colocar código del frontend en el backend.
- Mantener los archivos de infraestructura separados.
- Mantener la documentación dentro de `docs/`.
- Evitar carpetas innecesarias.
- Evitar archivos duplicados.
- Utilizar nombres descriptivos.
- Mantener una estructura coherente durante todo el desarrollo.

Si se propone modificar esta estructura, justificar primero el cambio.

---

## 5. Arquitectura del backend

El backend utilizará una arquitectura por capas y separación de responsabilidades.

Paquetes principales:

```text
backend/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── co/nalu/
    │   │       ├── api/
    │   │       ├── negocio/
    │   │       ├── datos/
    │   │       ├── integracion/
    │   │       └── config/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │           └── changelog/
    │
    └── test/
```

### Responsabilidades

**api/**
- Controladores REST.
- DTO de entrada y salida.
- Manejo de respuestas HTTP.
- Manejo centralizado de excepciones.

**negocio/**
- Reglas de negocio.
- Servicios.
- Validaciones de dominio.
- Operaciones transaccionales.

**datos/**
- Entidades JPA.
- Repositorios.
- Consultas de persistencia.

**integracion/**
- Servicios externos.
- Pasarelas de pago.
- Servicios de correo.
- Almacenamiento de archivos.

**config/**
- Configuración de Spring.
- Seguridad.
- JWT.
- CORS.
- Configuración general.

### Reglas del backend

- No colocar lógica de negocio en los controladores.
- No exponer entidades JPA directamente en las respuestas REST.
- Utilizar DTO.
- Utilizar transacciones cuando corresponda.
- Implementar paginación en consultas de listados.
- Evitar consultas N+1.
- Aplicar validaciones tanto en la entrada como en el dominio.
- Mantener las migraciones de base de datos versionadas con Liquibase.
- Utilizar pruebas de arquitectura para verificar las dependencias entre capas.

---

## 6. Arquitectura del frontend

El frontend será una aplicación Angular con renderizado del lado del servidor (SSR).

Responsabilidades:

- Experiencia de compra.
- Catálogo público.
- Búsqueda y filtros.
- Fichas de productos.
- Carrito.
- Checkout.
- Autenticación.
- Perfil del cliente.
- Administración.
- Gestión de pedidos.

### Reglas del frontend

- Separar componentes, servicios, modelos y funcionalidades.
- Evitar componentes excesivamente grandes.
- No duplicar lógica de negocio.
- Utilizar servicios para la comunicación HTTP.
- Centralizar el manejo de errores.
- Utilizar interceptores HTTP para las necesidades transversales.
- Mantener compatibilidad con SSR.
- No acceder directamente a APIs exclusivas del navegador durante el renderizado del servidor.
- Implementar interfaces adaptables a móviles, tabletas y escritorio.
- Mantener consistencia visual en toda la aplicación.
- Aplicar buenas prácticas de accesibilidad.

### Criterio visual

Nalu debe transmitir una identidad de marca de joyería profesional.

La interfaz debe priorizar:

- Diseño limpio y elegante.
- Jerarquía visual clara.
- Fotografías de productos de alta calidad.
- Tipografía consistente.
- Espaciado equilibrado.
- Navegación intuitiva.
- Experiencia móvil cuidada.
- Estados de carga, error y vacío bien diseñados.

No utilizar interfaces genéricas sin adaptación a la identidad de Nalu.

El propietario debe aprobar las decisiones visuales importantes.

---

## 7. Base de datos

Se utilizará PostgreSQL.

Liquibase será responsable de gestionar los cambios del esquema.

### Reglas

- Todas las modificaciones del esquema deben tener migraciones.
- No modificar manualmente la base de datos como sustituto de una migración.
- Utilizar claves primarias y foráneas.
- Definir restricciones e índices necesarios.
- Utilizar restricciones CHECK cuando sean pertinentes.
- Evitar eliminar datos históricos importantes.
- Utilizar transacciones para operaciones críticas.
- Diseñar correctamente las relaciones entre productos, variantes, materiales e inventario.

Las migraciones deben poder ejecutarse de manera reproducible desde una base de datos nueva.

---

## 8. Seguridad

La aplicación debe implementar seguridad desde las primeras fases.

### Autenticación

- Login.
- Access tokens JWT.
- Refresh tokens.
- Logout.
- Gestión de sesiones y revocación según el diseño aprobado.

### Autorización

- Roles y permisos.
- Protección de endpoints.
- Separación entre administración y clientes.
- Validación de permisos en el backend.

### Protección general

- Validar entradas.
- Proteger información sensible.
- No almacenar contraseñas en texto plano.
- Utilizar mecanismos seguros de almacenamiento de contraseñas.
- No registrar tokens ni secretos en logs.
- Utilizar variables de entorno para credenciales.
- Configurar CORS de forma restrictiva.
- Aplicar controles contra accesos no autorizados.
- Proteger los endpoints administrativos.

No confiar exclusivamente en las restricciones visuales del frontend para proteger recursos.

---

## 9. Metodología de trabajo

El desarrollo se realizará por fases.

Cada fase debe seguir este procedimiento:

1. Analizar los requisitos.
2. Revisar el estado actual del repositorio.
3. Identificar dependencias.
4. Dividir el trabajo en tareas.
5. Identificar tareas delegables.
6. Presentar al propietario las decisiones pendientes.
7. Implementar.
8. Ejecutar pruebas.
9. Revisar código.
10. Actualizar documentación.
11. Presentar resultados.
12. Solicitar aprobación para avanzar.

### Formato de seguimiento

Mantener un registro de tareas con:

- Identificador.
- Descripción.
- Responsable.
- Dependencias.
- Estado.
- Archivos afectados.
- Pruebas realizadas.
- Resultado.

Estados permitidos:

- PENDIENTE.
- EN PROGRESO.
- EN REVISIÓN.
- BLOQUEADO.
- COMPLETADO.

No marcar como COMPLETADO un trabajo que no haya sido verificado.

---

## 10. Coordinación multiagente

Claude Code es el agente principal.

Cuando existan herramientas y agentes auxiliares disponibles, podrá delegar trabajo independiente.

### Roles sugeridos

**Agente Backend**
- Implementación de servicios.
- Repositorios.
- Entidades.
- DTO.
- Pruebas unitarias.

**Agente Frontend**
- Componentes Angular.
- Servicios HTTP.
- Formularios.
- Interfaces.
- Pruebas frontend.

**Agente QA**
- Pruebas unitarias.
- Pruebas de integración.
- Casos límite.
- Revisión de errores.

**Agente DevOps**
- Docker.
- Docker Compose.
- CI/CD.
- Configuración de infraestructura.

**Agente Documentación**
- Documentación técnica.
- Diagramas.
- Manuales.
- Registro de decisiones.

### Reglas de delegación

- Los agentes deben recibir tareas concretas.
- No delegar tareas con dependencias no resueltas.
- Evitar que dos agentes modifiquen simultáneamente los mismos archivos.
- Claude debe revisar todo código recibido.
- Ningún agente puede cambiar decisiones arquitectónicas por iniciativa propia.
- No utilizar servicios de pago sin autorización.
- Priorizar herramientas gratuitas y disponibles localmente.
- Evitar consumir tokens innecesariamente.
- No duplicar análisis que ya estén documentados.

Si no hay agentes auxiliares disponibles, Claude debe continuar directamente sin bloquear el desarrollo.

---

## 11. Plan de desarrollo

### Fase 0 — Fundaciones

Objetivo: dejar funcionando la estructura técnica del proyecto.

Actividades:

- Inicialización del backend.
- Inicialización del frontend.
- Configuración de Docker Compose.
- Configuración de PostgreSQL.
- Configuración de Liquibase.
- Entidades iniciales.
- Validaciones YAVI.
- Estructura de autenticación JWT.
- Configuración de GitHub Actions.
- Pruebas iniciales.

Criterios de aceptación:

- Backend inicia correctamente.
- Frontend inicia correctamente.
- PostgreSQL funciona.
- Las migraciones se ejecutan.
- La configuración de seguridad está implementada.
- El pipeline CI funciona.
- Las pruebas iniciales pasan.

### Fase 1 — Cuentas y catálogo administrativo

Objetivo: permitir la administración de clientes y productos.

Actividades:

- Registro e inicio de sesión.
- Perfil de usuario.
- Direcciones.
- Categorías.
- Productos.
- Materiales.
- Variantes.
- Imágenes.

Criterio de aceptación:

Un administrador puede publicar un producto con dos materiales y tres tamaños.

### Fase 2 — Tienda pública y SEO

Objetivo: construir la experiencia pública de compra.

Actividades:

- Catálogo.
- Búsqueda.
- Filtros.
- Página de producto.
- Metadatos SEO.
- Datos estructurados JSON-LD.
- Renderizado SSR.

Criterio de aceptación:

El catálogo y las páginas de producto funcionan correctamente y tienen metadatos verificables.

### Fase 3 — Carrito y checkout

Objetivo: implementar el proceso de compra.

Actividades:

- Carrito de invitados.
- Carrito de clientes.
- Sincronización de carritos.
- Cotización.
- Cupones.
- Pedidos.
- Reserva de inventario.
- Control de concurrencia.

Criterio de aceptación:

Ante dos compras simultáneas de la última unidad disponible, solamente una puede obtener la reserva.

### Fase 4 — Pagos

Objetivo: integrar la pasarela de pagos.

Actividades:

- Integración con Wompi.
- Pagos de prueba.
- Webhooks.
- Verificación de firmas.
- Idempotencia.
- Expiración de reservas.
- Notificaciones.

Criterio de aceptación:

Los pagos aprobados, rechazados y los eventos duplicados se procesan correctamente.

### Fase 5 — Operación

Objetivo: implementar las herramientas administrativas posteriores a la compra.

Actividades:

- Movimientos de inventario.
- Gestión de pedidos.
- Estados de pedidos.
- Envíos.
- Historial.
- Reseñas.
- Reportes.
- Exportación CSV.

Criterio de aceptación:

Un pedido puede recorrer correctamente su ciclo de vida y los reportes reflejan las ventas.

### Fase 6 — Calidad y cierre

Objetivo: verificar la calidad integral de la aplicación.

Actividades:

- Pruebas unitarias.
- Pruebas de integración.
- Pruebas de seguridad.
- Pruebas de carga.
- Compatibilidad de navegadores.
- Verificación de accesibilidad.
- Copias de seguridad.
- Restauración.
- Documentación final.

Criterio de aceptación:

Informe de calidad, cobertura mínima objetivo del 60% y verificación de los requisitos no funcionales.

---

## 12. Decisiones de negocio pendientes

Claude no debe inventar las siguientes decisiones:

1. Fórmula de precios por material.
2. Tratamiento del peso del oro en el precio.
3. Costos de envío.
4. Envío gratuito por monto mínimo.
5. Cobertura geográfica de los envíos.
6. Tiempo de reserva de inventario.
7. Cancelación automática de pedidos pendientes de pago.
8. Política de devoluciones.
9. Almacenamiento definitivo de imágenes.
10. Hosting de producción.
11. Dominio definitivo.
12. Formato definitivo del número de pedido.

Cuando alguna de estas decisiones sea necesaria:

- Explicar por qué es necesaria.
- Presentar alternativas.
- Indicar ventajas y desventajas.
- Esperar aprobación del propietario.
- Registrar la decisión en `docs/decisiones/`.

---

## 13. Calidad del código

Todo código nuevo debe cumplir estas reglas:

- Ser legible.
- Tener responsabilidades claras.
- Evitar duplicación.
- Evitar complejidad innecesaria.
- Utilizar nombres descriptivos.
- Mantener consistencia.
- Incluir pruebas cuando corresponda.
- No introducir código muerto.
- No dejar errores conocidos sin documentar.
- No utilizar soluciones temporales como implementaciones definitivas.

Antes de terminar una tarea:

1. Compilar.
2. Ejecutar pruebas relevantes.
3. Revisar errores.
4. Revisar cambios de Git.
5. Actualizar documentación si corresponde.

---

## 14. Gestión de Git

Utilizar commits descriptivos siguiendo Conventional Commits.

Ejemplos:

- `feat: implement product catalog`
- `fix: correct inventory reservation`
- `refactor: separate order business logic`
- `test: add payment integration tests`
- `docs: update architecture documentation`
- `chore: configure development environment`

### Restricciones

- No hacer `git push --force` sin autorización.
- No borrar ramas sin autorización.
- No sobrescribir el repositorio remoto sin autorización.
- No incluir `.env` ni secretos en commits.
- No realizar commits de cambios ajenos a la tarea sin revisarlos.
- Revisar `git status` antes de modificar o confirmar cambios.

---

## 15. Documentación obligatoria

Mantener actualizados:

- `README.md`: descripción e instrucciones generales.
- `CLAUDE.md`: instrucciones y reglas del proyecto.
- `docs/arquitectura/`: arquitectura y diagramas.
- `docs/requisitos/`: requisitos funcionales y no funcionales.
- `docs/decisiones/`: decisiones arquitectónicas y comerciales aprobadas.
- `docs/diseno/`: documentos de análisis y diseño.

Las decisiones importantes deben registrarse mediante ADR (Architecture Decision Records).

---

## 16. Primera instrucción de ejecución

Al comenzar una sesión:

1. Leer completamente este archivo.
2. Revisar la estructura actual del repositorio.
3. Revisar el estado de Git.
4. Revisar la documentación disponible.
5. Identificar la fase actual.
6. Identificar las tareas pendientes.
7. No modificar archivos sin comprender su función.

### Instrucción inicial

Comienza trabajando exclusivamente en la Fase 0.

Primero analiza el repositorio y presenta:

- Estado actual del proyecto.
- Estructura existente.
- Tecnologías y versiones recomendadas.
- Dependencias necesarias.
- Tareas que puede ejecutar Claude.
- Tareas que pueden delegarse.
- Tareas que debe realizar el propietario.
- Decisiones pendientes.
- Criterios de aceptación.

**No empieces a programar inmediatamente. Primero presenta el diagnóstico y espera la aprobación del propietario.**

