package co.nalu.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Reloj único de la aplicación (sustituible en pruebas), tareas programadas y envío asíncrono de correos. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
