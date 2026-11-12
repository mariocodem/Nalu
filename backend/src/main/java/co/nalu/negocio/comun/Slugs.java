package co.nalu.negocio.comun;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** URL amigables: minúsculas, sin tildes y con guiones ("Anillos de plata" → "anillos-de-plata"). */
public final class Slugs {

    public static final String PATRON = "^[a-z0-9]+(-[a-z0-9]+)*$";
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");

    private Slugs() {}

    public static String generar(String texto) {
        var sinTildes = DIACRITICOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        var slug = NO_ALFANUMERICO.matcher(sinTildes.toLowerCase(Locale.ROOT)).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }

    /** Usa el slug indicado o, si viene vacío, lo genera a partir del nombre. */
    public static String elegir(String indicado, String nombre) {
        return indicado == null || indicado.isBlank() ? generar(nombre) : indicado.strip();
    }
}
