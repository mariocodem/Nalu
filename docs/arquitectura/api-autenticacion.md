# Contrato de la API de autenticación (F0-11 / F0-14)

Concreta el plan de desarrollo §8 para que backend y frontend se implementen en paralelo. Si algo cambia, se actualiza aquí primero.

Base: `/api/v1/auth`. Todas las respuestas de error son `application/problem+json` en español.

## Tipos

```ts
interface UsuarioSesion {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  roles: ('CLIENTE' | 'ADMINISTRADOR')[];
}

interface Sesion {
  accessToken: string;   // JWT HS256, 15 min. Solo en memoria del cliente
  expiraEn: string;      // instante ISO-8601 en UTC, p. ej. "2026-11-09T15:15:00Z"
  usuario: UsuarioSesion;
}
```

## Endpoints

| Método y ruta | Entrada | Respuesta correcta | Errores |
|---|---|---|---|
| `POST /login` | JSON `{ correo, contrasena }` | `200` `Sesion` + `Set-Cookie: nalu_rt` | `400` validación (con `errores`), `401` credenciales incorrectas o cuenta bloqueada |
| `POST /refresh` | Cookie `nalu_rt`, header `Origin` | `200` `Sesion` + `Set-Cookie: nalu_rt` (rotada) | `401` cookie ausente, expirada, revocada o reutilizada; `403` `Origin` no permitido |
| `POST /logout` | Cookie `nalu_rt` (opcional), header `Origin` | `204` + cookie borrada (`Max-Age=0`) | `403` `Origin` no permitido |
| `GET /me` | `Authorization: Bearer <accessToken>` | `200` `UsuarioSesion` | `401` sin token o token inválido |

- `/refresh` devuelve la `Sesion` completa (también el usuario) para que la recarga de página recupere la sesión con una sola llamada.
- El `401` de credenciales no distingue entre correo inexistente y contraseña incorrecta: `"Correo o contraseña incorrectos"`.

## Cookie del refresh token

`nalu_rt=<valor opaco>; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Max-Age=<segundos hasta expirar>` y `Secure` fuera del perfil `dev`.

## Reglas para el frontend

- El access token vive solo en memoria (un `signal`), nunca en `localStorage` ni `sessionStorage`.
- `Authorization: Bearer` solo en solicitudes a `/api/`, y nunca en `/api/v1/auth/login`, `/refresh` ni `/logout`.
- `withCredentials: true` solo en `/refresh` y `/logout`. En desarrollo el *proxy* de `ng serve` hace que todo sea del mismo origen.
- Ante un `401` en una solicitud con token: **un único** `/refresh` compartido entre solicitudes concurrentes y un solo reintento. Si el refresh falla, se cierra la sesión local y se navega a `/cuenta/login`.
- Al iniciar la app en el navegador se llama a `/refresh` para recuperar la sesión. Durante el renderizado en el servidor (SSR) no se llama: la cookie no viaja al servidor de Angular.
