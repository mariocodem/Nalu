package co.nalu.integracion.correo;

import co.nalu.negocio.notificaciones.Notificador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envía las notificaciones por SMTP (Mailpit en desarrollo). */
@Component
public class CorreoNotificador implements Notificador {

    private final JavaMailSender correo;
    private final String remitente;

    public CorreoNotificador(JavaMailSender correo, @Value("${nalu.correo.remitente}") String remitente) {
        this.correo = correo;
        this.remitente = remitente;
    }

    @Override
    public void enviar(Mensaje mensaje) {
        var m = new SimpleMailMessage();
        m.setFrom(remitente);
        m.setTo(mensaje.para());
        m.setSubject(mensaje.asunto());
        m.setText(mensaje.texto());
        correo.send(m);
    }
}
