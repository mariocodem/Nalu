# ADR 006: PayU Latam WebCheckout como pasarela de pagos (sandbox)

- **Estado:** Aceptada por el propietario el 2026-11-19
- **Fecha:** 2026-11-19
- **Relación:** complementa la Fase 4 y el punto P-04 del ADR 003; Wompi se conserva como alternativa

## Contexto

El propietario pidió integrar PayU Latam en modo sandbox para el proyecto universitario: debe ser funcional y limpio, no de nivel de producción. El proyecto ya tenía un flujo de pagos completo con Wompi: el puerto `PasarelaPagos`, intentos `Pago` con referencia única, un webhook firmado e idempotente y la confirmación o liberación del inventario.

Decisiones que planteó Claude y que el propietario aceptó:

| Decisión | Respuesta del propietario |
|---|---|
| Resultado del pago | Se verifica la firma de la página de respuesta; no se confía en el `transactionState` de la URL |
| Wompi | Se conserva, seleccionable por configuración |
| Pago rechazado | Se usan los estados existentes: el intento queda `RECHAZADO` y el pedido sigue `PENDIENTE_PAGO`, de modo que el cliente puede reintentar. No se crea `PAGO_RECHAZADO` en el pedido |
| Endpoint para iniciar el pago | Se reutilizan `POST /pedidos` y `POST /pedidos/{numero}/reintentar-pago`; no se crea `/pagos/iniciar/{pedidoId}` |

## Decisión

### Flujo

1. `POST /api/v1/pedidos` (o `reintentar-pago`) devuelve `DatosPago` con `urlPago` y los `campos` del formulario de WebCheckout, firmados en el servidor.
2. El navegador crea un `<form method="POST">` oculto con esos campos y lo envía a PayU. Los datos de la tarjeta nunca pasan por Nalu.
3. PayU devuelve al cliente a `responseUrl` (`/pago/resultado`) con el resultado en la URL (GET).
4. El frontend reenvía esos parámetros a `POST /api/v1/pagos/payu/respuesta`. El backend verifica la firma y procesa el resultado con la misma lógica del webhook: detección de repetidos, comprobación del monto, inventario, cupón y correos.

### Firmas (MD5 en hexadecimal minúscula)

- Formulario: `ApiKey~merchantId~referenceCode~amount~currency`. `amount` va en pesos sin decimales (`115000`).
- Respuesta: `ApiKey~merchantId~referenceCode~TX_VALUE~currency~transactionState`, con `TX_VALUE` redondeado a un decimal con el método mitad al par (`115000.00` → `115000.0`), según la documentación de PayU. Como los montos en COP son enteros, no aparecen los casos ambiguos (un 5 en el segundo decimal).

### Estados

| `transactionState` | Intento de pago | Pedido |
|---|---|---|
| 4 (aprobada) | `APROBADO` | `PAGADO`, salida de inventario |
| 6 (rechazada) | `RECHAZADO` | Sigue `PENDIENTE_PAGO`; se libera la reserva y se puede reintentar |
| 7 (pendiente) | `PENDIENTE` | Sigue `PENDIENTE_PAGO` con la reserva |
| 5 (expirada), 104 (error) | `ERROR` | Como el rechazo |

### Configuración

- `nalu.pagos.pasarela` (`NALU_PASARELA`): `payu` por defecto, o `wompi`. Solo se crea el adaptador activo; el webhook de Wompi solo existe con Wompi activa.
- `nalu.payu.*` (`PAYU_MERCHANT_ID`, `PAYU_ACCOUNT_ID`, `PAYU_API_KEY`, `PAYU_URL`, `PAYU_PRUEBA`). Por defecto usa las credenciales públicas del sandbox de PayU para Colombia (`508029` / `512321`), que PayU publica y no son secretas. La API key nunca llega al navegador.
- La CSP del servidor SSR (`form-action`) permite `sandbox.checkout.payulatam.com` y `checkout.payulatam.com`.

## Consecuencias

- La comprobación del monto, la idempotencia y el inventario son los mismos para las dos pasarelas: no se duplicó lógica de negocio.
- **Limitación:** sin la página de confirmación de PayU (webhook, que exige una URL pública), un pago solo se registra si el cliente vuelve a la tienda. Si cierra el navegador en PayU, o si el pago queda pendiente (PSE, efectivo) y se aprueba más tarde, Nalu no se entera, y el pedido se cancela a las 24 h. Para producción haría falta implementar `confirmationUrl`.
- En el sandbox, la API key es pública: la firma protege la integridad del flujo, pero no la confidencialidad de una cuenta real.
- Las pruebas de Wompi (`CompraTest`, `OperacionTest`) siguen corriendo con `nalu.pagos.pasarela=wompi`; las de PayU están en `PayuPasarelaTest` y `PagoPayuTest`.
