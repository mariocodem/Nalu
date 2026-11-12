---
version: 1
slug: "frontend-src-app-tienda-inicio-inicio-html"
primary_target: "frontend/src/app/tienda/inicio/inicio.html"
related_targets: ["frontend/src/app/shared/cabecera/cabecera.ts","frontend/src/styles.scss"]
---

# Inicio de la tienda (`/`)

Scope: rediseño del inicio y del marco público compartido (aviso, cabecera, pie, tarjeta de pieza). Modo: Persuade.
Audiencia: mujeres de 25 a 45 años en Colombia, mayoritariamente en el móvil, que compran para sí mismas.
Trabajo de la página: que entienda en segundos qué vende Nalu (joyería en acero, plata y oro, con esmeraldas y cuarzos), vea piezas reales con precio y entre al catálogo, a una categoría o a una pieza.
Prueba disponible: catálogo real de la API, fotografías de demostración (solo uso académico), condiciones de envío del ADR 003, pago con Wompi, reserva de 30 minutos.
Restricciones: sin newsletter (no existe integración), sin «más vendidos» (no hay dato público), sin política de devoluciones ni garantía (decisiones abiertas), sin afirmaciones de origen no documentadas.
Decisión del propietario (2026-11-09): mundo claro editorial para toda la tienda pública; reemplaza «La vitrina» (D-02).

## Direction contract

THESIS: La joya fotografiada en grande y la compra resuelta con precisión: una revista de joyería que se puede comprar. Rechaza el héroe partido con tarjeta al lado y la fila de tarjetas iguales.

OWN-WORLD: Blanco puro y piedra fría (#F3F2EF) como suelo de fotografía; tinta casi negra; un verde esmeralda profundo (#0E3B2E) que ocupa franjas completas (aviso, sala Esmeralda, pie), nunca acentos sueltos. Titulares en Bodoni Moda con óptica fija en 28 (la de 96 pierde los trazos finos en pantalla), interfaz en Hanken Grotesk. Esquinas rectas, reglas de 1px, sin sombras salvo en los paneles que se superponen a la página (menú, búsqueda, cuenta), botones de tinta sólida o de contorno fino.

STORY: Ve una pieza puesta a gran tamaño, entiende la oferta (metales y piedras), elige categoría o entra a la sala Esmeralda, revisa novedades con precio, confirma envío y pago, y compra.

FIRST VIEWPORT: Aviso esmeralda de 36px; cabecera blanca con menú «Joyería» a la izquierda, NALU centrado, búsqueda, cuenta y bolsa a la derecha; debajo una fotografía a sangre (cuello con cadena, blazer negro) de ~78vh con el titular en Bodoni blanco a la izquierda sobre la zona oscura y dos acciones: «Comprar ahora» (sólida) y «Ver novedades» (contorno). En móvil la foto es vertical y el texto se asienta abajo.

FORM: Revista de joyería comprable (dirección fijada por el brief del propietario: Mejuri, Blue Nile, Quince, Apple). Interacción firma: menú «Joyería» que se despliega con categorías y metales reales y una fotografía; movimiento: asentamiento lento de la foto principal (escala 1.04→1) y aparición del titular, respetando prefers-reduced-motion. seed: none (dirección fijada por el brief del propietario; no se tiró concept-seed)

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
