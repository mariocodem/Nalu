package co.nalu.negocio.clientes.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;

public record DireccionRequest(String etiqueta, String destinatario, String departamento, String ciudad,
                               String direccion, String codigoPostal, String telefono) {

    public DireccionRequest {
        etiqueta = limpiar(etiqueta);
        destinatario = limpiar(destinatario);
        departamento = limpiar(departamento);
        ciudad = limpiar(ciudad);
        direccion = limpiar(direccion);
        codigoPostal = codigoPostal == null || codigoPostal.isBlank() ? null : codigoPostal.strip();
        telefono = telefono == null ? null : telefono.replace(" ", "");
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.strip();
    }

    public static final Validator<DireccionRequest> ESQUEMA = EsquemaValidacion.para(DireccionRequest.class)
            ._string(DireccionRequest::etiqueta, "etiqueta", c -> c.notBlank().lessThanOrEqual(40))
            ._string(DireccionRequest::destinatario, "destinatario", c -> c.notBlank().lessThanOrEqual(120))
            ._string(DireccionRequest::departamento, "departamento", c -> c.notBlank().lessThanOrEqual(60))
            ._string(DireccionRequest::ciudad, "ciudad", c -> c.notBlank().lessThanOrEqual(60))
            ._string(DireccionRequest::direccion, "direccion", c -> c.notBlank().lessThanOrEqual(160))
            ._string(DireccionRequest::codigoPostal, "codigoPostal", c -> c.lessThanOrEqual(12))
            ._string(DireccionRequest::telefono, "telefono", c -> c.notBlank().pattern("^\\+?[0-9]{7,15}$")
                    .message("El teléfono debe tener entre 7 y 15 dígitos"))
            .build();
}
