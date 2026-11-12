package co.nalu.config;

import co.nalu.api.AuditoriaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

    private final AuditoriaInterceptor auditoria;

    WebConfig(AuditoriaInterceptor auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(auditoria).addPathPatterns("/api/v1/admin/**");
    }
}
