package co.nalu.datos.archivo;

/**
 * Almacén de archivos subidos (imágenes de producto). En desarrollo es el disco local; el
 * almacenamiento de producción está pendiente de decisión (CLAUDE.md §12, punto 9).
 */
public interface AlmacenamientoArchivos {

    /**
     * Guarda el contenido y devuelve la URL pública relativa con la que se sirve.
     *
     * @param carpeta subcarpeta lógica, p. ej. {@code productos}
     * @param extension sin punto, p. ej. {@code webp}
     */
    String guardar(String carpeta, byte[] contenido, String extension);

    /** Borra el archivo de una URL devuelta por {@link #guardar}. No falla si ya no existe. */
    void eliminar(String url);
}
