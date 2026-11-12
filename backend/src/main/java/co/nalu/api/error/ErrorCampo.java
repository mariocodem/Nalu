package co.nalu.api.error;

/**
 * Error de validación de un campo de la solicitud, tal como se publica en la propiedad
 * {@code errores} de la respuesta {@code problem+json}.
 *
 * @param campo ruta del campo, p. ej. {@code correo} o {@code items[0].cantidad}
 * @param mensaje explicación en español, lista para mostrar al usuario
 */
public record ErrorCampo(String campo, String mensaje) {}
