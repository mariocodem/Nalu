package co.nalu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Solo en dev y demo: fuera de ellos no se exigen las variables del administrador inicial. */
@Configuration(proxyBeanMethods = false)
@Profile({"dev", "demo"})
@EnableConfigurationProperties(AdministradorInicialProperties.class)
class SemillaConfig {}
