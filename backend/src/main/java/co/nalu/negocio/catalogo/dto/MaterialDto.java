package co.nalu.negocio.catalogo.dto;

import java.math.BigDecimal;

public record MaterialDto(Long id, String tipo, String ley, boolean requiereCertificado, BigDecimal factorPrecio) {}
