# ADR 005: Asistente de compras con IA (Gemini)

- **Estado:** Aceptada por el propietario el 2026-11-12
- **Fecha:** 2026-11-12

## Contexto

El propietario pidió un chat con IA que ayude al cliente a comprar. Respondió a las decisiones planteadas:

| Decisión | Respuesta del propietario |
|---|---|
| Capacidades | Asesorar y buscar en el catálogo; proponer añadir a la bolsa; consultar los pedidos del cliente con sesión |
| Proveedor y modelo | Gemini (Google), modelo configurable con `GEMINI_MODEL` (hoy `gemini-flash-latest`) |
| Credencial | `GEMINI_API_KEY` en `.env`, solo en el servidor |
| Acceso | Todos los visitantes, con límite de mensajes |

## Decisión

### Arquitectura

| Capa | Pieza | Responsabilidad |
|---|---|---|
| `negocio.asistente` | `ModeloConversacional` (puerto) | Contrato independiente del proveedor: instrucciones, conversación, herramientas y ejecutor |
| `negocio.asistente` | `AsistenteService` | Instrucciones del asistente, herramientas y su ejecución con los servicios existentes. No abre transacción: la llamada al modelo es lenta |
| `negocio.asistente` | `LimitadorAsistente` | Ventana deslizante por cuenta o IP (`NALU_ASISTENTE_MENSAJES`, 20 cada 10 min por defecto) |
| `integracion.gemini` | `GeminiModeloConversacional` | API REST `generateContent` de Gemini con llamadas a funciones, sin SDK nuevo (usa `RestClient` de Spring) |
| `api.publico` | `AsistenteController` | `GET /api/v1/asistente/estado` y `POST /api/v1/asistente/mensajes` (contrato: `docs/arquitectura/api-asistente.md`) |
| Frontend | `shared/asistente` | Botón «Asesora» y panel de chat; tarjetas de piezas con «Añadir a la bolsa» |

### Herramientas del modelo

`buscar_piezas`, `ver_pieza`, `opciones_catalogo`, `condiciones_compra` y `mostrar_piezas` para todos; `mis_pedidos` y `ver_pedido` solo si llega un access token con rol `CLIENTE`. El id del cliente sale del token, nunca de lo que diga el modelo.

### Reglas de datos

- **Precios, tallas y disponibilidad salen del catálogo.** Las tarjetas del chat se arman en el backend con `CatalogoPublicoService.ficha`; el texto del modelo no puede crear una tarjeta ni cambiar un precio. Si el modelo nombra piezas sin llamar a `mostrar_piezas`, el backend muestra las que encontró y nombró.
- **El asistente no actúa.** No añade a la bolsa ni crea pedidos: la persona pulsa el botón de la tarjeta, que usa el flujo existente de la bolsa.
- **Pedidos sin datos personales.** `ver_pedido` envía al proveedor número, estado, piezas, totales, transportadora y guía; nunca la dirección, el teléfono ni el correo.
- **La conversación vive en el navegador** (`sessionStorage` de la pestaña). El servidor no la guarda.
- La respuesta se pinta como texto (negrita y listas), nunca como HTML.

### Disponibilidad del proveedor

En las pruebas del 2026-11-12 `gemini-flash-latest` respondió 503 («high demand») o no respondió en 90 s varias veces seguidas; `gemini-flash-lite-latest` respondió en unos 5 s. Por eso:

- Cada llamada tiene un límite de 15 s. Si el modelo principal falla por saturación (429/5xx), no existe (404) o no responde, se usan los de `GEMINI_MODELOS_RESPALDO`.
- Un modelo que falla queda en pausa 2 minutos y los turnos siguientes van directo al respaldo.
- A mitad de un turno no se cambia de modelo: las firmas de razonamiento (`thoughtSignature`) que Gemini adjunta a cada llamada a función solo valen para el modelo que las produjo.
- Si nada responde, la API devuelve 503 y el chat muestra «no disponible» con un botón para reintentar. Sin llave, el botón «Asesora» no aparece.

## Consecuencias

- **Costo:** cada mensaje consume tokens de la cuenta de Google del propietario. Cada llamada envía las instrucciones, las declaraciones de herramientas y el historial, y un mensaje puede hacer varias llamadas si el modelo usa herramientas. El consumo por llamada se registra en el log con nivel DEBUG (`logging.level.co.nalu.integracion.gemini=DEBUG`). Solo se midió la prueba mínima (205 tokens). El límite por persona acota el abuso, pero no el gasto total.
- **Privacidad (Ley 1581):** lo que escribe el cliente y los datos que devuelven las herramientas se envían a Google como encargado del tratamiento. El chat avisa que las respuestas se generan con Google Gemini y pide no compartir datos personales. Antes de producción hay que:
  - mencionar este tratamiento en la política de datos;
  - revisar los términos vigentes de la Gemini API, porque las condiciones de uso de los datos cambian entre el nivel gratuito y el de pago.
- **Límite por instancia:** el limitador vive en la memoria de cada instancia del backend. Detrás de un proxy hay que configurar las cabeceras reenviadas (`server.forward-headers-strategy`) para que la IP sea la del cliente; con el proxy de desarrollo de Angular todos los visitantes comparten la IP `127.0.0.1` y, por tanto, el límite.
- **El modelo puede equivocarse en lo general.** Por ejemplo, puede explicar mal cómo cuidar una pieza. Los datos comerciales están protegidos por las reglas anteriores; el aviso del chat pide confirmar precio y disponibilidad en la ficha.
- Política de devoluciones y garantía: siguen sin definirse (CLAUDE.md §12). El asistente tiene instrucciones de no prometer nada sobre ellas.
