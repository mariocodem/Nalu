# Contrato de la API del asistente de compras (ADR 005)

Base `/api/v1/asistente`. Público: sin token responde a un visitante; con un access token de rol `CLIENTE` el asistente también consulta los pedidos de ese cliente. Errores en `application/problem+json`.

## `GET /estado`

```ts
interface EstadoAsistente { disponible: boolean }   // false si no hay GEMINI_API_KEY
```

El frontend solo muestra el botón «Asesora» si `disponible` es `true`.

## `POST /mensajes`

La conversación vive en el navegador: cada mensaje viaja con el historial anterior.

```ts
interface SolicitudAsistente {
  historial?: { rol: 'cliente' | 'asistente'; texto: string }[];  // máx. 20 mensajes, cada texto 1–4000 caracteres
  mensaje: string;                                                // 1–1000 caracteres
}
interface RespuestaAsistente {
  texto: string;           // markdown ligero: **negrita** y listas; el frontend nunca lo inserta como HTML
  piezas: PiezaSugerida[]; // tarjetas armadas con el catálogo (máx. 4)
}
interface PiezaSugerida {
  slug: string; nombre: string; imagen: { url: string; textoAlternativo: string } | null;
  material: string | null; precioDesde: number | null; agotado: boolean;
  variantes: { id: number; talla: string | null; precio: number; disponible: boolean }[];
}
```

| Respuesta | Cuándo |
|---|---|
| `200 RespuestaAsistente` | — |
| `400` con `errores` | Mensaje vacío o largo, historial de más de 20 mensajes o con un `rol` distinto de `cliente`/`asistente` |
| `429` | La persona (cuenta, o IP si no hay sesión) superó `NALU_ASISTENTE_MENSAJES` mensajes en 10 minutos |
| `503` (`urn:nalu:error:asistente-no-disponible`) | Sin llave configurada, o Gemini no respondió con ningún modelo |

Para añadir una pieza de una tarjeta a la bolsa, el frontend usa `POST /api/v1/carrito/items` con el `id` de la variante (contrato de la Fase 3); el asistente nunca modifica la bolsa.
