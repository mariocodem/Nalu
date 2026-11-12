package co.nalu.negocio.comun;

import am.ik.yavi.core.ViolationMessage;
import am.ik.yavi.message.MessageFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Traduce al español los mensajes por defecto de YAVI.
 *
 * <p>Las plantillas viven en {@code validacion/mensajes.properties} (UTF-8) con las mismas claves
 * que {@code am.ik.yavi.core.ViolationMessage.Default}. El argumento {@code {0}} es el nombre del
 * campo.
 *
 * <p>Un mensaje personalizado con {@code .message(...)} en el esquema tiene prioridad. YAVI conserva
 * la clave por defecto en ese caso, así que la plantilla en español solo se aplica cuando el formato
 * recibido sigue siendo el original de YAVI (en inglés).
 */
public final class FormateadorMensajesValidacion implements MessageFormatter {

    public static final Locale ESPANOL_COLOMBIA = Locale.of("es", "CO");

    static final String RECURSO = "/validacion/mensajes.properties";

    /** Formato original (en inglés) de YAVI para cada clave. */
    private static final Map<String, String> FORMATOS_ORIGINALES = Arrays.stream(ViolationMessage.Default.values())
            .collect(Collectors.toUnmodifiableMap(ViolationMessage.Default::messageKey,
                    ViolationMessage.Default::defaultMessageFormat));

    private static final FormateadorMensajesValidacion INSTANCIA = new FormateadorMensajesValidacion(cargar());

    private final Properties plantillas;

    private FormateadorMensajesValidacion(Properties plantillas) {
        this.plantillas = plantillas;
    }

    public static FormateadorMensajesValidacion instancia() {
        return INSTANCIA;
    }

    /** Indica si existe una plantilla en español para la clave de YAVI. */
    public boolean tienePlantilla(String claveMensaje) {
        return plantillas.containsKey(claveMensaje);
    }

    @Override
    public String format(String claveMensaje, String formatoPorDefecto, Object[] argumentos, Locale locale) {
        boolean esMensajePersonalizado = !formatoPorDefecto.equals(FORMATOS_ORIGINALES.get(claveMensaje));
        String plantilla = esMensajePersonalizado
                ? formatoPorDefecto
                : plantillas.getProperty(claveMensaje, formatoPorDefecto);
        return new MessageFormat(plantilla, ESPANOL_COLOMBIA).format(argumentos);
    }

    private static Properties cargar() {
        try (InputStream entrada = FormateadorMensajesValidacion.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró " + RECURSO);
            }
            var propiedades = new Properties();
            propiedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return propiedades;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
    }
}
