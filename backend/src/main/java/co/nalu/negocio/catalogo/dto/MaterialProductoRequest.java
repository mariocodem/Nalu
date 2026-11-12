package co.nalu.negocio.catalogo.dto;

import am.ik.yavi.core.Validator;
import co.nalu.datos.entidad.RolMaterial;
import co.nalu.negocio.comun.EsquemaValidacion;
import java.math.BigDecimal;

public record MaterialProductoRequest(Long idMaterial, RolMaterial rol, BigDecimal pesoGramos, BigDecimal quilates) {

    public static final Validator<MaterialProductoRequest> ESQUEMA = EsquemaValidacion.para(MaterialProductoRequest.class)
            ._long(MaterialProductoRequest::idMaterial, "idMaterial", c -> c.notNull())
            ._object(MaterialProductoRequest::rol, "rol", c -> c.notNull())
            ._bigDecimal(MaterialProductoRequest::pesoGramos, "pesoGramos", c -> c.greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("1000000")))
            ._bigDecimal(MaterialProductoRequest::quilates, "quilates", c -> c.greaterThan(BigDecimal.ZERO)
                    .lessThan(new BigDecimal("10000")))
            .build();
}
