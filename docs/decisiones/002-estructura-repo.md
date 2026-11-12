# ADR 002: Estructura del repositorio

- **Estado:** Aprobada por el propietario el 2026-11-09
- **Fecha:** 2026-11-09

## Contexto

Las carpetas creadas a mano no coincidían con `CLAUDE.md`: había `infraestructura/` donde el documento decía `infrastructure/`, y `github/` en lugar de `.github/`. Además, `/home/solary/Nalu` no tenía repositorio Git propio y colgaba del repositorio de `/home/solary`.

## Decisión

- Repositorio Git propio en `/home/solary/Nalu`, rama `main`, todavía sin remoto.
- Se mantiene `infraestructura/` en español, coherente con el resto del proyecto. `CLAUDE.md` y `README.md` se corrigieron.
- `github/` pasa a `.github/workflows/`, la única ruta que lee GitHub Actions.
- Las decisiones se registran en `docs/decisiones/` y no en `docs/adr/`, como decía el plan técnico.
- El plan técnico se movió de la raíz a `docs/arquitectura/plan-desarrollo.md`.
- Se añadió `docs/diseno/` para el PDF de análisis y diseño.

## Consecuencias

- La lista maestra de decisiones de negocio pendientes es la de `CLAUDE.md` §12.
