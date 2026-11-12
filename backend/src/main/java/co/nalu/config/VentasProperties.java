package co.nalu.config;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros comerciales de la venta (ADR 003).
 *
 * @param tarifaEnvio tarifa plana nacional
 * @param envioGratisDesde subtotal (después de descuentos) desde el que el envío es gratis
 * @param reservaTtl tiempo que se reservan las existencias mientras se paga
 * @param cancelacionSinPago tiempo tras el que un pedido sin pago aprobado se cancela solo
 */
@Validated
@ConfigurationProperties("nalu.ventas")
public record VentasProperties(
        @NotNull @DefaultValue("15000") BigDecimal tarifaEnvio,
        @NotNull @DefaultValue("200000") BigDecimal envioGratisDesde,
        @NotNull @DefaultValue("30m") Duration reservaTtl,
        @NotNull @DefaultValue("24h") Duration cancelacionSinPago) {}
