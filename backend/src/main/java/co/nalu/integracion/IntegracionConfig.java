package co.nalu.integracion;

import co.nalu.integracion.gemini.GeminiProperties;
import co.nalu.integracion.wompi.WompiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({WompiProperties.class, GeminiProperties.class})
class IntegracionConfig {}
