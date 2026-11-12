package co.nalu.config;

import co.nalu.datos.archivo.AlmacenamientoLocal;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sirve los archivos subidos (imágenes de producto) desde el almacenamiento local. */
@Configuration(proxyBeanMethods = false)
class ArchivosConfig implements WebMvcConfigurer {

    private final Path raiz;

    ArchivosConfig(@Value("${nalu.archivos.directorio}") Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler(AlmacenamientoLocal.PREFIJO_URL + "**")
                .addResourceLocations(raiz.toUri().toString())
                .setCachePeriod(60 * 60 * 24 * 30); // los nombres son UUID: el contenido nunca cambia
    }
}
