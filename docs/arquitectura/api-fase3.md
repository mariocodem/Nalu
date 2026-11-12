# Contrato de la API de las Fases 3 y 4 (carrito, checkout y pagos)

Complementa `api-fase2.md`. Reglas de negocio: plan §6.3–§6.6 y `docs/decisiones/003-decisiones-de-negocio.md`. Todos los importes los calcula el backend.

## 1. Carrito (RF08)

Un visitante se identifica con el header `X-Cart-Token` (lo devuelve la primera respuesta en `token`); un cliente autenticado (rol `CLIENTE`), con su access token. Un administrador compra como visitante.

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `GET /carrito` | — | `200 Carrito` (vacío si no hay) |
| `POST /carrito/items` | `{ idVariante, cantidad (1–20) }` | `200 Carrito`. Suma si la variante ya está. `409` si supera las existencias libres; `404` si la pieza no está a la venta |
| `PATCH /carrito/items/{id}` | `{ cantidad }` | `200 Carrito` |
| `DELETE /carrito/items/{id}` | — | `200 Carrito` |
| `POST /carrito/fusionar` | header `X-Cart-Token` (cliente autenticado) | `200 Carrito`: el carrito del visitante pasa a la cuenta, sumando ítems con el tope de 20 |

```ts
interface Carrito { token: string | null; items: ItemCarrito[]; cantidadTotal: number; subtotal: number }
interface ItemCarrito {
  id: number; idVariante: number; slug: string; nombre: string; talla: string | null;
  imagen: string | null; textoAlternativo: string | null;
  precioUnitario: number;   // precio vigente, recalculado en cada respuesta
  cantidad: number; subtotal: number;
  maximo: number;           // unidades que se pueden pedir (existencias libres, tope 20)
  disponible: boolean;      // false si se retiró o se agotó: no suma al subtotal
}
```

## 2. Checkout (RF09–RF11, cliente)

- `POST /checkout/cotizar` `{ codigoCupon? }` → `Cotizacion { lineas[], subtotal, descuento, costoEnvio, total, cupon: { codigo, aplicado, mensaje } | null, faltantes: string[] }`. No crea ni reserva nada.
- `POST /pedidos` `{ idDireccion, codigoCupon? }` → `201 PedidoCreado { numero, estado: "PENDIENTE_PAGO", total, reservaExpiraEn, pago: DatosPago }`.
  - Una sola transacción: bloqueo de las variantes en orden de id, verificación de existencias (`409` con `faltantes[]`), precios del servidor, cupón (`409` si no aplica), reserva de 30 min, pedido con historial y pago `INICIADO`, carrito `CONVERTIDO`.
  - Envío: $15.000, gratis si `subtotal − descuento ≥ $200.000`.
  - Número `NALU-XXXXXX` (6 caracteres sin 0/O/1/I/L).
- `DatosPago { urlPago, referencia, montoCentavos, moneda }`: el navegador va a `urlPago` (checkout web de Wompi, con la firma de integridad). Referencia `NUMERO-intento`.

## 3. Pagos (RF12)

- `POST /webhooks/wompi` (público, firmado). Verifica `signature.checksum` = SHA-256(valores de `signature.properties` + `timestamp` + secreto de eventos). Firma inválida → `401`. Evento repetido (`idTransaccion:estado`) → `200` sin efecto.
  - `APPROVED` con el monto exacto: salida de inventario (movimiento `SALIDA` por línea), pedido `PAGADO`, uso del cupón, correo.
  - `DECLINED`/`VOIDED`/`ERROR`: libera la reserva; el cliente puede reintentar. `PENDING`: no toca el inventario.
  - Monto distinto: el pago queda en `ERROR` y el pedido sigue pendiente.
- `POST /pedidos/{numero}/reintentar-pago` → `DatosPago` nuevo; si la reserva expiró, vuelve a verificar y reservar (`409` con `faltantes`).
- Tareas: cada minuto se liberan las reservas vencidas; cada 15 minutos se cancelan los pedidos sin pago aprobado con más de 24 horas.

## 4. Pedidos del cliente (RF14)

- `GET /cuenta/pedidos?page&size` → `Pagina<{ numero, fecha, estado, total, piezas }>`.
- `GET /cuenta/pedidos/{numero}` → detalle con líneas (precio histórico y descuento prorrateado), totales, cupón, dirección, historial, `estadoPago`, `puedePagar`, `reservaExpiraEn` y `envio`. Un pedido ajeno → `404`.

## 5. Cupones (administración, RF11)

`GET|POST /admin/promociones`, `PUT /admin/promociones/{id}` con `{ codigo, tipo: PORCENTAJE|VALOR_FIJO, valor, fechaInicio, fechaFin, usosMaximos?, activa }`. El código no cambia al editar. El uso se cuenta cuando el pago se aprueba.
