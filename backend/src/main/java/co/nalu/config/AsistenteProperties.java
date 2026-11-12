package co.nalu.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Límites del asistente de compras (ADR 005): cada mensaje cuesta tokens del proveedor.
 *
 * @param mensajesPorVentana mensajes que una misma persona (cuenta o IP) puede enviar por ventana
 * @param ventana duración de la ventana deslizante
 */
@Validated
@ConfigurationProperties("nalu.asistente")
public record AsistenteProperties(
        @Positive @DefaultValue("20") int mensajesPorVentana,
        @NotNull @DefaultValue("10m") Duration ventana) {}
