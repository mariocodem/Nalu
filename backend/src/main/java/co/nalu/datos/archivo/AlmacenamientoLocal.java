package co.nalu.datos.archivo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda los archivos en disco bajo {@code nalu.archivos.directorio} y los publica en
 * {@value #PREFIJO_URL}. Los nombres son UUID generados aquí: nunca se usa el nombre que envía el
 * cliente, lo que evita recorridos de ruta.
 */
@Component
public class AlmacenamientoLocal implements AlmacenamientoArchivos {

    public static final String PREFIJO_URL = "/archivos/";
    private static final Pattern SEGMENTO_SEGURO = Pattern.compile("^[a-z0-9-]+$");

    private final Path raiz;

    public AlmacenamientoLocal(@Value("${nalu.archivos.directorio}") Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, byte[] contenido, String extension) {
        if (!SEGMENTO_SEGURO.matcher(carpeta).matches() || !SEGMENTO_SEGURO.matcher(extension).matches()) {
            throw new IllegalArgumentException("Carpeta o extensión no válidas");
        }
        var nombre = UUID.randomUUID() + "." + extension;
        try {
            var destino = raiz.resolve(carpeta);
            Files.createDirectories(destino);
            Files.write(destino.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return PREFIJO_URL + carpeta + "/" + nombre;
    }

    @Override
    public void eliminar(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }
        var ruta = raiz.resolve(url.substring(PREFIJO_URL.length())).normalize();
        if (!ruta.startsWith(raiz)) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar el archivo", e);
        }
    }
}
