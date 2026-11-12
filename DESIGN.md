---
name: Nalu
description: Joyería colombiana en acero, plata y oro, con esmeraldas y cuarzos; una revista de joyería que se puede comprar.
colors:
  fondo: "#ffffff"
  fondo-2: "#f8f7f5"
  piedra: "#f3f2ef"
  regla: "#cfccc6"
  regla-suave: "#e7e5e1"
  tinta: "#141414"
  tinta-hover: "#2e2e2e"
  tinta-2: "#55524d"
  tinta-3: "#6b675f"
  esmeralda-profunda: "#0e3b2e"
  esmeralda-profunda-2: "#134a3a"
  sobre-esmeralda: "#f4f1ea"
  sobre-esmeralda-2: "#b9cbc2"
  esmeralda: "#0f6b4f"
  error: "#a3301b"
typography:
  display:
    fontFamily: "'Bodoni Moda', 'Didot', 'Bodoni 72', serif"
    fontSize: "clamp(2.9rem, 7.4vw, 6rem)"
    fontWeight: 400
    lineHeight: 0.98
    letterSpacing: "-0.02em"
    fontVariation: "'opsz' 28"
  display-italica:
    fontFamily: "'Bodoni Moda', 'Didot', 'Bodoni 72', serif"
    fontSize: "clamp(3.2rem, 7vw, 6rem)"
    fontWeight: 400
    lineHeight: 0.95
    fontVariation: "'opsz' 28"
  headline:
    fontFamily: "'Bodoni Moda', 'Didot', 'Bodoni 72', serif"
    fontSize: "clamp(2rem, 3.8vw, 3.1rem)"
    fontWeight: 400
    lineHeight: 1.08
    letterSpacing: "-0.01em"
    fontVariation: "'opsz' 28"
  title:
    fontFamily: "'Bodoni Moda', 'Didot', 'Bodoni 72', serif"
    fontSize: "clamp(1.6rem, 2.6vw, 2.2rem)"
    fontWeight: 400
    lineHeight: 1.1
    fontVariation: "'opsz' 28"
  marca:
    fontFamily: "'Bodoni Moda', 'Didot', 'Bodoni 72', serif"
    fontSize: "clamp(1.45rem, 2.2vw, 1.85rem)"
    fontWeight: 500
    lineHeight: 1
    letterSpacing: "0.34em"
    fontVariation: "'opsz' 28"
  body:
    fontFamily: "'Hanken Grotesk', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.55
  pieza-nombre:
    fontFamily: "'Hanken Grotesk', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.98rem"
    fontWeight: 500
    lineHeight: 1.35
  boton:
    fontFamily: "'Hanken Grotesk', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.95rem"
    fontWeight: 600
    letterSpacing: "0.01em"
  nav:
    fontFamily: "'Hanken Grotesk', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.9rem"
    fontWeight: 500
  label:
    fontFamily: "'Hanken Grotesk', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    letterSpacing: "0.12em"
rounded:
  ninguno: "0"
  conteo: "999px"
spacing:
  gutter: "clamp(16px, 4vw, 56px)"
  ritmo: "clamp(72px, 9vw, 136px)"
  ancho: "90rem"
  rejilla: "clamp(0.75rem, 1.6vw, 1.5rem)"
  franja: "clamp(3.5rem, 7vw, 6.5rem)"
components:
  button-primary:
    backgroundColor: "{colors.tinta}"
    textColor: "{colors.fondo}"
    typography: "{typography.boton}"
    rounded: "{rounded.ninguno}"
    padding: "0.75rem 1.6rem"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.tinta-hover}"
    textColor: "{colors.fondo}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.tinta}"
    typography: "{typography.boton}"
    rounded: "{rounded.ninguno}"
    padding: "0.75rem 1.6rem"
    height: "48px"
  button-secondary-hover:
    backgroundColor: "{colors.tinta}"
    textColor: "{colors.fondo}"
  button-claro:
    backgroundColor: "{colors.fondo}"
    textColor: "{colors.tinta}"
    typography: "{typography.boton}"
    rounded: "{rounded.ninguno}"
    padding: "0.75rem 1.6rem"
    height: "48px"
  button-claro-hover:
    backgroundColor: "{colors.piedra}"
    textColor: "{colors.tinta}"
  button-contorno-claro:
    backgroundColor: "transparent"
    textColor: "{colors.fondo}"
    typography: "{typography.boton}"
    rounded: "{rounded.ninguno}"
    padding: "0.75rem 1.6rem"
    height: "48px"
  button-contorno-claro-hover:
    backgroundColor: "{colors.fondo}"
    textColor: "{colors.tinta}"
  input:
    backgroundColor: "{colors.fondo}"
    textColor: "{colors.tinta}"
    typography: "{typography.body}"
    rounded: "{rounded.ninguno}"
    padding: "0.65rem 0.85rem"
    height: "46px"
  aviso-envio:
    backgroundColor: "{colors.esmeralda-profunda}"
    textColor: "{colors.sobre-esmeralda}"
    padding: "0.55rem 0"
  cabecera:
    backgroundColor: "{colors.fondo}"
    textColor: "{colors.tinta}"
    height: "64px"
  vitrina-pieza:
    backgroundColor: "{colors.piedra}"
    textColor: "{colors.tinta}"
    typography: "{typography.pieza-nombre}"
    rounded: "{rounded.ninguno}"
  sala-esmeralda:
    backgroundColor: "{colors.esmeralda-profunda}"
    textColor: "{colors.sobre-esmeralda}"
    typography: "{typography.display-italica}"
    padding: "{spacing.franja}"
  pie:
    backgroundColor: "{colors.esmeralda-profunda}"
    textColor: "{colors.sobre-esmeralda}"
    padding: "clamp(3.5rem, 8vw, 6rem) 0 2rem"
---

# Design System: Nalu

## Overview

**Creative North Star: "La revista que se puede comprar"**

Nalu se presenta como una revista de joyería: la fotografía manda, a gran tamaño y a sangre, sobre suelos blancos y de piedra fría; la compra se resuelve con la precisión sobria de una tienda bien hecha. Los titulares en Bodoni Moda ponen la voz editorial; Hanken Grotesk lleva toda la interfaz (navegación, precios, botones, formularios) sin adornos.

El color es casi todo tinta sobre blanco. El único color de marca, un esmeralda profundo, no aparece como acento suelto: ocupa franjas completas de lado a lado (el aviso de envío, la sala Esmeralda, el pie) y el bloque de la sala dentro del menú «Joyería». La forma es rectilínea: esquinas rectas, reglas de 1px y superficies planas en reposo. El movimiento es lento y asentado (curva de salida larga), y se apaga por completo con `prefers-reduced-motion`.

Densidad editorial: secciones separadas por un ritmo vertical amplio, rejillas de fotografías en proporción 4:5 y listas regladas en lugar de filas de tarjetas.

Alcance: este sistema se extrajo de la tienda pública (aviso, cabecera, menú «Joyería», búsqueda, menú de cuenta, menú móvil, inicio, tarjeta de pieza y pie). El panel de administración y las páginas de cuenta heredan los tokens y las utilidades globales (`.campo`, `.boton`, `.tabla`, `.panel-seccion`) pero no se rediseñaron; no se documentan aquí patrones propios de esas pantallas.

**Key Characteristics:**
- Fotografía grande sobre blanco puro y piedra fría; la imagen es la protagonista.
- Tinta casi negra para texto y para toda acción interactiva.
- Esmeralda profundo solo en franjas completas, más el anillo de foco y el estado de éxito.
- Bodoni Moda en titulares con óptica fija 28; Hanken Grotesk en la interfaz.
- Esquinas rectas, reglas de 1px, superficies planas.
- Movimiento lento con una sola curva de salida.

## Colors

Paleta de tinta sobre blanco y piedra, con un único color de marca que se usa en bloque.

### Primary
- **Esmeralda profunda** (esmeralda-profunda): suelo de las franjas completas: aviso de envío (36px), sala Esmeralda del inicio, bloque de la sala dentro del menú «Joyería» y pie. También es el fondo de la selección de texto.
- **Esmeralda profunda iluminada** (esmeralda-profunda-2): solo el hover del bloque de la sala en el menú «Joyería».
- **Marfil sobre esmeralda** (sobre-esmeralda): texto principal sobre cualquier franja esmeralda.
- **Salvia sobre esmeralda** (sobre-esmeralda-2): texto secundario sobre franjas esmeralda (bajadas, títulos de grupo del pie, material en tarjetas de la sala, aviso legal).

### Secondary
- **Esmeralda de señal** (esmeralda): anillo de foco global (2px, desplazado 3px), estado de éxito (`.aviso--exito`, confirmaciones en la ficha). No se usa para botones, enlaces ni hover.

### Neutral
- **Tinta** (tinta): todo el texto principal y todas las acciones: botón sólido, contorno, subrayados de navegación, contador de la bolsa, regla superior de las listas. `--acento` es un alias de tinta.
- **Tinta en hover** (tinta-hover): hover del botón sólido.
- **Tinta media** (tinta-2): texto de apoyo (definiciones, descripciones de condiciones, etiquetas de campo, hover de iconos).
- **Tinta suave** (tinta-3): metadatos y marcadores de posición (material en tarjeta, títulos de grupo del menú, placeholder, hover de campo).
- **Blanco** (fondo): suelo principal, cabecera, paneles desplegables, texto sobre la fotografía de portada.
- **Blanco cálido** (fondo-2): hover de filas en el menú de cuenta.
- **Piedra fría** (piedra): suelo de fotografía (tarjetas y categorías antes de cargar la imagen), esqueletos de carga, franja «Comprar en Nalu», hover del botón claro.
- **Regla** (regla): bordes de campo y reglas entre filas de listas.
- **Regla suave** (regla-suave): borde inferior de la cabecera, bordes de paneles desplegables, separadores de estado.
- **Error** (error): bordes de campo inválido y mensajes de error; los avisos de error usan este tono a 35% en el borde y 5% en el fondo.

### Named Rules
**The Franja Completa Rule.** El esmeralda profundo ocupa franjas o bloques completos, nunca un botón, un enlace, un icono o un subrayado. Si un elemento de interacción necesita color, es tinta.

**The Tinta Actúa Rule.** Toda acción interactiva (botón, enlace, navegación activa, casilla) se pinta en tinta; sobre franjas oscuras o fotografía se invierte a blanco (botón claro o de contorno claro).

## Typography

**Display Font:** Bodoni Moda (con Didot, Bodoni 72, serif), autoalojada, pesos 400 a 600 e itálica 400.
**Body Font:** Hanken Grotesk (con system-ui, -apple-system, Segoe UI, sans-serif), autoalojada, pesos 400 a 700.

**Character:** un Didone de alto contraste da la voz de revista; un grotesco limpio y neutro hace el trabajo de tienda. Nunca se mezclan dentro de un mismo bloque de texto.

### Hierarchy
- **Display** (400, clamp 2.9 a 6rem, interlineado 0.98, -0.02em): titular de la portada, máximo 11ch, en blanco sobre la zona oscura de la foto.
- **Display itálica** (400 itálica, clamp 3.2 a 6rem, 0.95): nombre de la sala «Esmeralda». La itálica se reserva para la sala (inicio y menú «Joyería»).
- **Headline** (400, clamp 2 a 3.1rem, 1.08, -0.01em): títulos de sección. Los títulos de los bloques de dos columnas (metales, condiciones) suben a clamp 2.2 a 3.8rem con 1.02.
- **Title** (400, clamp 1.6 a 2.2rem, 1.1): nombres en listas regladas (metales, condiciones); los nombres de categoría usan clamp 1.35 a 1.75rem y los enlaces de categoría del menú 1.6rem.
- **Marca** (500, clamp 1.45 a 1.85rem, 0.34em, mayúsculas): logotipo tipográfico NALU centrado en la cabecera; en el pie crece a clamp 2.6 a 3.6rem con 0.3em.
- **Body** (400, 16px, 1.55): texto corrido; bajadas limitadas a 30 a 46ch.
- **Nombre de pieza** (500, 0.98rem, 1.35): nombre del producto en la tarjeta; el precio va a 0.95rem con cifras tabulares.
- **Botón** (600, 0.95rem, 0.01em): texto de todos los botones, en caja normal.
- **Nav** (500, 0.9rem): enlaces de la cabecera.
- **Label** (600, 0.75rem, 0.12em, mayúsculas): solo como encabezado de un grupo de enlaces (columnas del menú «Joyería», menú móvil, pie) y en cabeceras de tabla (0.8rem, 0.06em).

### Named Rules
**The Óptica 28 Rule.** Todo Bodoni (h1 a h3, marca, nombres editoriales) usa `font-optical-sizing: none` con `'opsz' 28`. Las ópticas altas pierden los trazos finos en pantalla; no subir la óptica aunque el tamaño crezca.

**The Una Voz Por Bloque Rule.** Bodoni para nombrar (titulares, categorías, metales, la sala); Hanken para todo lo que se lee para comprar (precios, materiales, botones, formularios). El nombre de una pieza en la tarjeta es Hanken.

## Layout

Contenedor de 90rem (`ancho`) centrado con márgenes laterales fluidos (`gutter`, 16 a 56px). Las secciones se separan con un ritmo vertical fluido (`ritmo`, 72 a 136px). Las franjas de color (sala Esmeralda, condiciones de compra, pie) rompen el contenedor y van de borde a borde, con el contenido interior alineado al mismo ancho.

- **Portada:** fotografía a sangre de clamp(540px, 78svh, 820px) de alto (500 a 700px en móvil), texto anclado abajo a la izquierda sobre un velo degradado negro al 50% en el 60% inferior.
- **Rejillas de fotografía:** 4 columnas en escritorio, 2 en tablet y móvil; espacio horizontal `rejilla` (0.75 a 1.5rem), vertical clamp 2 a 3rem en novedades. Todas las fotos van en 4:5.
- **Bloques de dos columnas:** título a la izquierda, lista reglada a la derecha (1fr / 1.3fr, espacio clamp 2 a 6rem); se apilan bajo 64rem.
- **Sala Esmeralda:** texto (0.9fr) y tres piezas (2fr); bajo 48rem las piezas pasan a un carrusel horizontal con scroll-snap al 68% de ancho.
- **Cabecera:** rejilla de tres columnas (navegación, marca centrada, acciones), 64px de alto (56px bajo 64rem), fija arriba.
- **Puntos de corte:** 80rem (oculta las categorías sueltas de la navegación), 64rem (cabecera móvil, apilado de bloques), 48rem (rejillas a 2 columnas, carrusel de la sala), 30rem (recorta el aviso de envío).

## Elevation & Depth

El sistema es plano. Las secciones, tarjetas, botones y campos no llevan sombra; la profundidad se construye con franjas de color (blanco, piedra, esmeralda) y reglas de 1px. La única elevación es la de los paneles que flotan sobre la página al abrirse desde la cabecera, y es una sombra difusa y baja, nunca desplazada.

### Shadow Vocabulary
- **Panel desplegado** (`box-shadow: 0 30px 60px -40px rgb(20 20 20 / 0.35)`): menú «Joyería» y panel de búsqueda, que bajan a todo el ancho desde la cabecera.
- **Menú flotante** (`box-shadow: 0 18px 40px -18px rgb(20 20 20 / 0.22)`): menú de cuenta.

### Named Rules
**The Plano En Reposo Rule.** Nada que esté en el flujo de la página lleva sombra. La sombra solo separa un panel que se superpone al contenido.

## Shapes

Esquinas rectas en todo (radio 0): botones, campos, tarjetas, fotografías, paneles, franjas. Los bordes son de 1px (regla, regla suave o tinta); las listas regladas abren con una regla de tinta y separan sus filas con regla. La única forma redondeada es el contador de la bolsa (píldora de 18px). Los iconos son trazo de 1.5px, sin relleno, con remates redondeados, a 22px en la cabecera y 18px en enlaces con flecha.

## Components

### Buttons
Rectángulos sólidos o de contorno fino, sin redondeo, con el peso puesto en el texto.
- **Shape:** esquinas rectas (0), alto mínimo 48px (36px en la variante pequeña), borde de 1px.
- **Primario / oscuro:** fondo tinta, texto blanco, relleno 0.75rem 1.6rem.
- **Secundario:** transparente con borde y texto tinta; en hover se rellena de tinta.
- **Claro (sobre fotografía o esmeralda):** fondo blanco, texto tinta; hover a piedra.
- **Contorno claro (sobre fotografía):** transparente, borde blanco al 70%, texto blanco; hover se rellena de blanco.
- **Enlace:** sin caja, subrayado de 1px desplazado 0.25em, peso 500.
- **Hover / Focus / Active:** transiciones de 0.25s con la curva del sistema; al pulsar baja 1px; foco con el anillo esmeralda global (blanco sobre fondos oscuros); deshabilitado al 45% de opacidad.

### Cards / Containers
- **Tarjeta de pieza:** fotografía 4:5 sobre piedra (blanco cuando la tarjeta va sobre esmeralda), sin borde ni sombra, sin relleno interno. Debajo, a 0.9rem: nombre (Hanken 500), material (tinta suave) y «Desde» con precio. Hover: la foto escala a 1.035 en 0.9s y el nombre se subraya. Marca «Agotada» en una placa blanca en la esquina superior izquierda. Sin foto: «Fotografía pendiente» centrado en tinta suave.
- **Categoría:** misma fotografía 4:5 sobre piedra con el nombre en Bodoni debajo; hover escala la foto a 1.04 en 1s y subraya el nombre.
- **Lista reglada:** filas separadas por reglas en lugar de tarjetas (metales con «Ver piezas» a la derecha; condiciones de compra como lista de definiciones). Las filas enlazadas se desplazan 0.75rem a la derecha en hover.

### Inputs / Fields
- **Style:** fondo blanco, borde de 1px en regla, radio 0, alto mínimo 46px, etiqueta encima en 0.875rem 600 tinta media.
- **Hover:** el borde pasa a tinta suave.
- **Focus:** borde tinta con un segundo trazo interior de 1px (borde visual de 2px), sin anillo exterior.
- **Error:** borde y mensaje en el tono de error.
- **Búsqueda de cabecera:** campo sin caja, solo una regla inferior de tinta que engrosa a 2px con el foco, texto de 1.1 a 1.35rem y botón primario a la derecha.

### Navigation
- **Aviso de envío:** franja esmeralda de 36px, texto de 0.8rem centrado; bajo 30rem se recorta a la primera frase.
- **Cabecera:** blanca, borde inferior en regla suave. Enlaces en Hanken 0.9rem 500; en hover o abiertos, un subrayado de 1px crece desde la izquierda en 0.4s. Iconos de búsqueda, cuenta y bolsa con área de 44px; hover a tinta media.
- **Menú «Joyería»:** panel a todo el ancho que se despliega de arriba abajo (clip-path, 0.45s). Tres columnas: categorías en Bodoni 1.6rem, materiales en Hanken, y el bloque de la sala Esmeralda (foto cuadrada, nombre en Bodoni itálica 2.6rem, enlace subrayado).
- **Menú móvil:** bajo 64rem, el botón de tres líneas abre un panel a pantalla completa con categorías en Bodoni 1.85rem, materiales y cuenta en Hanken 1.05rem.
- **Pie:** franja esmeralda con la marca NALU grande y columnas de enlaces; aviso legal sobre una regla marfil al 16%.

### Sala Esmeralda (signature)
Franja esmeralda a todo el ancho: nombre «Esmeralda» en Bodoni itálica display, una frase en salvia, botón claro, y tres tarjetas de pieza con foto sobre blanco. Es el único lugar del inicio donde el color de marca ocupa una sección entera.

### Estados de carga, error y vacío
Carga: esqueletos en piedra (foto 4:5 y dos líneas) que laten al 55% de opacidad cada 1.6s, con un aviso solo para lectores de pantalla. Error y vacío: un bloque abierto por una regla suave, mensaje en tinta media y, en error, un botón secundario «Reintentar».

## Do's and Don'ts

### Do:
- **Do** poner la fotografía primero: proporción 4:5 sobre piedra para producto y categoría, y a sangre en la portada.
- **Do** usar tinta para toda acción; sobre fotografía o esmeralda, invertir a botón claro o de contorno claro.
- **Do** usar el esmeralda profundo como franja o bloque completo, con texto marfil y salvia.
- **Do** fijar Bodoni en `'opsz' 28` con `font-optical-sizing: none` en cualquier tamaño.
- **Do** separar con reglas de 1px y listas regladas antes que con cajas.
- **Do** animar con `cubic-bezier(0.16, 1, 0.3, 1)` y duraciones largas (0.25s en controles, 0.4 a 1s en subrayados y fotos, 2.2s en la portada), y apagar todo con `prefers-reduced-motion`.

### Don't:
- **Don't** usar esmeralda en botones, enlaces, iconos, subrayados o hover; solo en franjas, en el anillo de foco y en el estado de éxito.
- **Don't** redondear esquinas; la única excepción es el contador de la bolsa.
- **Don't** poner sombra a nada que esté en el flujo de la página; la sombra es solo de paneles superpuestos.
- **Don't** poner el nombre de una pieza, un precio o un botón en Bodoni.
- **Don't** usar etiquetas en mayúsculas espaciadas encima de un titular; ese estilo es solo para encabezar grupos de enlaces y columnas de tabla.
