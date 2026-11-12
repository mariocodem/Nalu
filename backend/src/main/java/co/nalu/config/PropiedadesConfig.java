package co.nalu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades tipadas de Nalu. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class, VentasProperties.class,
        AsistenteProperties.class})
class PropiedadesConfig {}
