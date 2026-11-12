# ADR 001: Versiones del stack

- **Estado:** Aceptada. El propietario aprobó estas versiones con el plan e instaló el JDK 25 (2026-11-09)
- **Fecha:** 2026-11-09

## Contexto

`CLAUDE.md` §3 pide fijar versiones estables y compatibles antes de generar los proyectos, sin asumir que la versión instalada en local es la correcta. El 9 de noviembre de 2026, el equipo local tiene OpenJDK 27 (no LTS), Maven 3.10.0 y Node 26.13.

## Decisión

| Pieza | Versión | Motivo |
|---|---|---|
| Java | 25 (LTS) | La LTS vigente, soportada por Spring Boot 4.x. La 27 no es LTS |
| Spring Boot | 4.1.x (último parche al generar) | Estable desde junio de 2026. La 4.2 se evaluará cuando sea GA y haya madurado |
| Maven | Wrapper `mvnw` | Así la build no depende del Maven local |
| Angular | 22.x (último parche) | La versión mayor vigente. Angular publica ahora una versión mayor al año |
| Node | La LTS que soporte Angular 22, fijada con `.nvmrc` y `engines` | Builds reproducibles en local y en CI |
| PostgreSQL | 18 | La 19 sale el 19 de noviembre de 2026. Es demasiado nueva para un MVP |
| Liquibase, Hibernate, Testcontainers | Las que gestiona el BOM de Spring Boot 4.1 | Compatibilidad garantizada |
| YAVI, ArchUnit, springdoc-openapi, JaCoCo (0.8.14 o superior) | Su última versión estable compatible con Java 25 | Fuera del BOM; se fijan en el `pom.xml` |

## Versiones fijadas (2026-11-09)

Generadas en las tareas F0-04 (backend) y F0-13 (frontend).

| Pieza | Versión exacta | Dónde se fija |
|---|---|---|
| Java | 25.0.5 (OpenJDK) | `<java.version>25</java.version>` en `backend/pom.xml` |
| Spring Boot | 4.1.2 | padre `spring-boot-starter-parent` |
| Spring Framework / Spring Security | 7.0.10 / 7.1.2 | BOM de Spring Boot |
| Hibernate ORM | 7.4.13.Final | BOM de Spring Boot |
| Liquibase | 5.0.4 | BOM de Spring Boot |
| Driver PostgreSQL | 42.7.14 | BOM de Spring Boot |
| JUnit Jupiter / Testcontainers | 6.0.3 / 2.0.5 | BOM de Spring Boot |
| ArchUnit | 1.5.1 | propiedad `archunit.version` |
| Maven | 3.10.0 | `backend/.mvn/wrapper/maven-wrapper.properties` |
| Angular (framework) | 22.2.5 | `frontend/package.json` (`^22.2.0`) |
| Angular CLI, `@angular/build`, `@angular/ssr` | 22.2.6 | `frontend/package.json` |
| TypeScript | 6.0.3 | `frontend/package.json` |
| Pruebas del frontend | Vitest 5.1.0 (builder `@angular/build:unit-test`, jsdom) | Valor por defecto de Angular 22; sustituye a Karma |
| Node | 26 (LTS desde octubre de 2026), probado con 26.13.0 | `frontend/.nvmrc` y `engines` |
| PostgreSQL | 18.6 | `docker-compose.yml` y `TestcontainersConfiguration` (`postgres:18`) |

El frontend se generó en modo *zoneless* (sin `zone.js`), que es el modo recomendado de Angular 22.

YAVI se añade al `pom.xml` en F0-10. springdoc-openapi y JaCoCo se añaden con el primer endpoint documentado y con el primer umbral de cobertura, respectivamente.

## Riesgos

- `liquibase-hibernate` puede no ser compatible con Hibernate 7. La tarea F0-07 lo comprueba. Plan B: escribir los changesets YAML a mano a partir de las entidades, manteniendo `ddl-auto=validate`.

## Consecuencias

- El propietario debe instalar el JDK 25 en local (tarea P-01).
- La CI usa `actions/setup-java` con Java 25 y la versión de Node fijada.
