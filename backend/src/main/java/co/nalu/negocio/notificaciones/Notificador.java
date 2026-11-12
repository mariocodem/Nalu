package co.nalu.negocio.notificaciones;

/** Puerto de notificaciones al cliente; lo implementa {@code integracion.correo}. */
public interface Notificador {

    record Mensaje(String para, String asunto, String texto) {}

    void enviar(Mensaje mensaje);
}
