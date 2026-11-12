# Contrato de la API de la Fase 5 (operación)

Complementa `api-fase3.md`. Todo es de administración (rol `ADMINISTRADOR`) salvo la creación de reseñas.

## 1. Pedidos (RF14)

- `GET /admin/pedidos?estado&desde&hasta&q&page&size` → `Pagina<{ numero, fecha, cliente, correo, estado, total, piezas }>`. Fechas `AAAA-MM-DD` (zona America/Bogota, ambos días incluidos); `q` busca en el número y el correo.
- `GET /admin/pedidos/{numero}` → `{ pedido: PedidoDetalle, cliente: { nombre, correo, telefono }, pagos[], siguientes: EstadoPedido[] }`.
- `PATCH /admin/pedidos/{numero}/estado` `{ estado, nota? }`. Sigue la máquina de estados del plan §6.6:
  - `PAGADO` lo pone solo el webhook (`409`); `ENVIADO` solo se alcanza con `/envio` (`409`).
  - `EN_PREPARACION` desde `PAGADO`; `ENTREGADO` desde `ENVIADO` (fija `envio.fechaEntrega`).
  - `CANCELADO`: sin pagar, libera la reserva; pagado o en preparación, exige `nota` (`409` sin ella) y reintegra las unidades con un movimiento `ENTRADA` (el reembolso se gestiona fuera de Nalu).
- `POST /admin/pedidos/{numero}/envio` `{ transportadora, numeroGuia, costo? }` → registra el envío y pasa a `ENVIADO` (solo desde `EN_PREPARACION`). Envía un correo al cliente.

## 2. Inventario (RF13)

- `POST /admin/inventario/movimientos` `{ idVariante, tipo: ENTRADA|SALIDA|AJUSTE, cantidad, motivo }` → `201 Movimiento`. `cantidad` es positiva en entradas y salidas, y con signo en ajustes. Bloquea la fila y rechaza (`409`) dejar menos existencias que las reservadas.
- `GET /admin/inventario/movimientos?idVariante&page&size` → `Pagina<{ id, fecha, idVariante, sku, producto, talla, tipo, cantidad (con signo), motivo, responsable ("Sistema" o el administrador), numeroPedido }>`, del más reciente al más antiguo.
- `GET /admin/inventario/alertas?umbral=2` → variantes activas de productos no archivados con `disponibles ≤ umbral`.

## 3. Reseñas (RF07)

- `POST /productos/{slug}/resenas` (cliente) `{ calificacion 1–5, comentario? ≤1000 }` → `201`. Solo si el cliente tiene un pedido `ENTREGADO` con el producto (`409` si no) y una sola vez por producto (`409`). Queda `PENDIENTE`.
- `GET /admin/resenas?estado` y `PATCH /admin/resenas/{id}` `{ estado: APROBADA|RECHAZADA }`.
- La ficha (`GET /productos/{slug}`) incluye `resenas: { promedio, total, resenas[{ autor: "Ana P.", calificacion, comentario, fecha }] }` (solo las aprobadas). El JSON-LD añade `aggregateRating`.

## 4. Reportes (RF15)

`GET /admin/reportes/{tipo}?desde&hasta&formato=json|csv` → `{ titulo, columnas[], filas[][], resumen{} }` o un CSV (UTF-8 con BOM, separador coma). Cuentan como venta los pedidos `PAGADO`, `EN_PREPARACION`, `ENVIADO` y `ENTREGADO`.

| Tipo | Contenido |
|---|---|
| `ventas` | Unidades y valor por material del metal principal; resumen con pedidos, ingresos, descuentos, envíos y ticket promedio |
| `rotacion` | Piezas más vendidas (unidades y valor) |
| `agotados` | Variantes activas sin existencias libres |
| `cupones` | Pedidos y descuento total por cupón |
| `pagos` | Intentos de pago por estado y monto |

Los valores que empiezan por `=`, `+`, `-` o `@` llevan un apóstrofo delante, para evitar la inyección de fórmulas en la hoja de cálculo.
