package co.nalu.negocio.promociones.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.TipoPromocion;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PromocionDtos {

    private PromocionDtos() {}

    public record Promocion(Long id, String codigo, TipoPromocion tipo, BigDecimal valor, LocalDate fechaInicio,
                            LocalDate fechaFin, Integer usosMaximos, int usosActuales, boolean activa) {}

    public record PromocionRequest(String codigo, TipoPromocion tipo, BigDecimal valor, LocalDate fechaInicio,
                                   LocalDate fechaFin, Integer usosMaximos, boolean activa) {

        public PromocionRequest {
            codigo = codigo == null ? null : codigo.strip().toUpperCase(java.util.Locale.ROOT);
        }

        public static final Validator<PromocionRequest> ESQUEMA = EsquemaValidacion.para(PromocionRequest.class)
                ._string(PromocionRequest::codigo, "codigo", c -> c.notBlank().pattern("^[A-Z0-9-]{3,30}$")
                        .message("El código debe tener de 3 a 30 letras, números o guiones"))
                ._object(PromocionRequest::tipo, "tipo", c -> c.notNull())
                ._bigDecimal(PromocionRequest::valor, "valor", c -> c.notNull().greaterThan(BigDecimal.ZERO)
                        .lessThan(new BigDecimal("10000000000")))
                ._object(PromocionRequest::fechaInicio, "fechaInicio", c -> c.notNull())
                ._object(PromocionRequest::fechaFin, "fechaFin", c -> c.notNull())
                ._integer(PromocionRequest::usosMaximos, "usosMaximos", c -> c.greaterThan(0))
                .constraintOnTarget(p -> p.tipo() != TipoPromocion.PORCENTAJE || p.valor() == null
                                || p.valor().compareTo(BigDecimal.valueOf(100)) <= 0,
                        "valor", "porcentaje.maximo", "Un porcentaje no puede superar 100")
                .constraintOnTarget(p -> p.fechaInicio() == null || p.fechaFin() == null
                                || !p.fechaFin().isBefore(p.fechaInicio()),
                        "fechaFin", "rango.fechas", "La fecha final debe ser igual o posterior a la inicial")
                .build();
    }
}
