# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

**Principal:** personas —principalmente mujeres de 25 a 45 años— que compran joyería para sí mismas, tanto de uso diario como para ocasiones especiales. Compran en línea, en Colombia, y en su mayoría desde el móvil. Necesitan elegir la pieza, el material y la talla con confianza sin ver la joya en persona.

**Secundario:** el administrador de la tienda, que publica productos con sus variantes (material × talla), gestiona el inventario, los pedidos y los envíos, y consulta reportes.

El regalo no es el escenario prioritario; no se diseña el recorrido alrededor de él.

## Product Purpose

Tienda en línea y panel administrativo de una joyería multimaterial (acero, plata, oro, esmeraldas, cuarzo) que cubre el ciclo completo de una venta: catálogo → carrito → pedido con reserva de existencias → pago por Wompi → gestión del pedido y envío → reportes.

Éxito: que una clienta encuentre una pieza, entienda en qué se diferencia cada material y complete la compra sin dudas; y que el administrador opere la tienda sin depender de herramientas externas.

## Positioning

**Origen colombiano.** Las esmeraldas y el trabajo local o artesanal son el eje de la marca. Una joyería genérica importada no puede reclamar esto con verdad. Cómo se demuestra ese origen (taller, procedencia de las piedras, artesanos) aún no está documentado y no debe inventarse.

## Operating Context

- Venta exclusivamente en Colombia, en pesos colombianos (COP), interfaz en español.
- Pagos mediante el widget de Wompi; los datos de pago nunca pasan por el backend de Nalu.
- La guía de envío se registra manualmente; no hay integración con transportadoras.
- Las páginas públicas (inicio, catálogo, ficha) se renderizan en el servidor para SEO; las rutas con sesión se renderizan en el cliente.
- Tratamiento de datos personales bajo la Ley 1581 (aceptación de política en el registro y solicitud de supresión).

## Capabilities and Constraints

- Productos con variantes por material y talla; cada variante tiene su propio inventario.
- El precio y la disponibilidad los calcula siempre el backend; el frontend no calcula nada que afecte dinero o stock.
- Carrito de visitante y de cliente con fusión al iniciar sesión, cupones, reserva de inventario con control de concurrencia.
- Panel administrativo separado de la tienda, protegido por rol.
- Stack fijado en `CLAUDE.md` y `docs/decisiones/001-versiones.md` (Angular 22 con SSR + SCSS; Spring Boot 4.1; PostgreSQL 18).
- Fuera del MVP: app móvil nativa, pagos reales en producción, facturación electrónica, marketplaces, fidelización.

**Decisiones abiertas** (ver `CLAUDE.md` §12; no inventar): fórmula de precio por material, tratamiento del peso del oro, costos de envío y envío gratuito, cobertura geográfica, tiempo de reserva, cancelación automática, política de devoluciones, almacenamiento de imágenes, hosting, dominio, formato del número de pedido.

## Brand Commitments

- Nombre: **Nalu**. No existe logotipo, paleta, tipografía ni guía de voz definidos.
- Toda decisión visual importante requiere aprobación del propietario (`CLAUDE.md` §6).

## Evidence on Hand

- No hay fotografías de producto, testimonios, reseñas, prensa ni cifras de ventas.
- No hay información verificable sobre artesanos, talleres ni procedencia de las esmeraldas.
- Mientras falten, los diseños usan marcadores explícitos de imagen y texto; nunca fotos de stock presentadas como piezas reales ni afirmaciones de origen sin respaldo.

## Product Principles

1. **El origen se demuestra, no se decora.** Lo colombiano vive en hechos concretos (piedra, oficio, procedencia) cuando existan, no en clichés folclóricos.
2. **La pieza manda.** La fotografía y la información del producto ocupan el protagonismo; la interfaz se retira.
3. **Elegir material y talla sin dudas.** La diferencia entre acero, plata y oro —precio, cuidado, durabilidad— se entiende en la ficha, no en una página aparte.
4. **Confianza al pagar.** Totales, disponibilidad y estado del pedido siempre claros y provenientes del backend.
5. **Móvil primero.** La compra completa debe sentirse cuidada en un teléfono.

## Accessibility & Inclusion

RNF09: `alt` obligatorio en imágenes, formularios con `<label>`, foco visible, contraste WCAG AA; auditoría con Lighthouse/axe en cada fase.
