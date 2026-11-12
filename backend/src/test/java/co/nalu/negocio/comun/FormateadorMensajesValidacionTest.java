package co.nalu.negocio.comun;

import static org.assertj.core.api.Assertions.assertThat;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ViolationMessage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FormateadorMensajesValidacionTest {

    record Registro(String nombre, String correo, Integer cantidad) {}

    private static final am.ik.yavi.core.Validator<Registro> ESQUEMA = EsquemaValidacion.para(Registro.class)
            ._string(Registro::nombre, "nombre", c -> c.notBlank().lessThanOrEqual(5))
            ._string(Registro::correo, "correo", c -> c.notBlank().email().message("Ingresa un correo válido"))
            ._integer(Registro::cantidad, "cantidad", c -> c.notNull().greaterThan(0))
            .build();

    @Test
    void todasLasClavesDeYaviTienenTraduccion() {
        var formateador = FormateadorMensajesValidacion.instancia();
        var sinTraducir = Arrays.stream(ViolationMessage.Default.values())
                .map(ViolationMessage.Default::messageKey)
                .filter(clave -> !formateador.tienePlantilla(clave))
                .toList();
        assertThat(sinTraducir).isEmpty();
    }

    @Test
    void losMensajesPorDefectoSalenEnEspanol() {
        var violaciones = ESQUEMA.validate(new Registro("demasiado largo", "no-es-correo", 0),
                FormateadorMensajesValidacion.ESPANOL_COLOMBIA);

        assertThat(violaciones).extracting(ConstraintViolation::name, ConstraintViolation::message)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("nombre",
                                "El campo «nombre» admite como máximo 5 elementos o caracteres (tiene 15)"),
                        org.assertj.core.groups.Tuple.tuple("correo", "Ingresa un correo válido"),
                        org.assertj.core.groups.Tuple.tuple("cantidad", "El campo «cantidad» debe ser mayor que 0"));
    }

    @Test
    void campoObligatorio() {
        var violaciones = ESQUEMA.validate(new Registro(" ", "a@b.co", null),
                FormateadorMensajesValidacion.ESPANOL_COLOMBIA);

        assertThat(violaciones).extracting(ConstraintViolation::message)
                .containsExactlyInAnyOrder("El campo «nombre» es obligatorio", "El campo «cantidad» es obligatorio");
    }
}
