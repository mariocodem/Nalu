# ADR 003: Decisiones de negocio para la venta (CLAUDE.md §12)

- **Estado:** Aceptada por el propietario el 2026-11-09
- **Fecha:** 2026-11-09

## Contexto

Las Fases 2 a 4 (tienda, checkout y pagos) necesitaban decisiones que `CLAUDE.md` §12 prohíbe inventar. El propietario las tomó entre las alternativas presentadas.

## Decisiones

| # §12 | Tema | Decisión | Dónde se aplica |
|---|---|---|---|
| 1 y 2 | Fórmula de precio | `precio_venta(variante) = precio_base × factor_precio(material METAL_PRINCIPAL) + precio_adicional`, redondeado a pesos enteros (`HALF_UP`). Sin metal principal, el factor es 1. El peso del oro no entra en la fórmula: se refleja en el factor y en el precio base | `negocio.catalogo.CalculadoraPrecio` |
| 3 y 4 | Costo de envío | Tarifa plana nacional de **$15.000** y envío **gratis desde $200.000** de subtotal (después de descuentos). Los dos valores se configuran con `NALU_ENVIO_TARIFA` y `NALU_ENVIO_GRATIS_DESDE` | `negocio.pedidos` (cotización) |
| 5 | Cobertura | Toda Colombia | Validación de la dirección en el checkout |
| 6 | Reserva de inventario | **30 minutos** (`RESERVA_TTL_MINUTOS`) | `negocio.pedidos` |
| 7 | Pedidos sin pagar | Se cancelan solos a las **24 horas** sin un pago aprobado | Tarea programada de `negocio.pedidos` |
| 12 | Número de pedido | Código aleatorio `NALU-XXXXXX`: 6 caracteres de un alfabeto sin caracteres confusos (sin 0/O ni 1/I/L), único en la base de datos | `negocio.pedidos` |
| P-04 | Pasarela | La integración con Wompi se construye y prueba con eventos simulados según su documentación; el pago real en sandbox se verifica cuando el propietario ponga las llaves en `.env` | `integracion.wompi` |

## Pendientes

Siguen abiertas: política de devoluciones (8), almacenamiento definitivo de imágenes (9), hosting (10) y dominio (11). No bloquean el desarrollo; sí la puesta en producción.
