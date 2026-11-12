package co.nalu.negocio.clientes.dto;

import am.ik.yavi.core.Validator;
import co.nalu.negocio.comun.EsquemaValidacion;

public record PerfilRequest(String nombre, String apellido, String telefono, String documento,
                            boolean aceptaComunicaciones) {

    public PerfilRequest {
        nombre = nombre == null ? null : nombre.strip();
        apellido = apellido == null ? null : apellido.strip();
        telefono = telefono == null ? null : telefono.replace(" ", "");
        documento = documento == null || documento.isBlank() ? null : documento.strip();
    }

    public static final Validator<PerfilRequest> ESQUEMA = EsquemaValidacion.para(PerfilRequest.class)
            ._string(PerfilRequest::nombre, "nombre", c -> c.notBlank().lessThanOrEqual(60))
            ._string(PerfilRequest::apellido, "apellido", c -> c.notBlank().lessThanOrEqual(60))
            ._string(PerfilRequest::telefono, "telefono", c -> c.notBlank().pattern("^\\+?[0-9]{7,15}$")
                    .message("El teléfono debe tener entre 7 y 15 dígitos"))
            ._string(PerfilRequest::documento, "documento", c -> c.lessThanOrEqual(20))
            .build();
}
