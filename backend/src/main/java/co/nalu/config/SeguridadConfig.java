package co.nalu.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * API sin estado autenticada con JWT HS256 (plan de desarrollo §8.4).
 *
 * <p>CSRF desactivado: la API solo acepta credenciales en el header {@code Authorization}. La única
 * cookie ({@code nalu_rt}) es {@code SameSite=Strict}, se limita a {@code /api/v1/auth} y esos
 * endpoints verifican además el header {@code Origin} (§8.5).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, ProblemasSeguridad problemas) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/productos/*/resenas").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos", "/api/v1/productos/**",
                                "/api/v1/categorias", "/api/v1/categorias/**", "/api/v1/materiales",
                                "/api/v1/materiales/**", "/api/v1/seo/rutas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/asistente/estado").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/asistente/mensajes").permitAll() // pedidos solo con token de cliente
                        .requestMatchers(HttpMethod.POST, "/api/v1/carrito/fusionar").hasRole("CLIENTE")
                        .requestMatchers("/api/v1/carrito", "/api/v1/carrito/**").permitAll() // visitante con X-Cart-Token
                        .requestMatchers("/api/v1/checkout/**", "/api/v1/pedidos/**").hasRole("CLIENTE")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/cuenta/**").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/archivos/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRoles()))
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemas)
                        .accessDeniedHandler(problemas))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        var decoder = NimbusJwtDecoder.withSecretKey(props.claveSecreta()).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.emisor()));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return NimbusJwtEncoder.withSecretKey(props.claveSecreta()).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties app) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(app.origenFrontend()));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Cart-Token"));
        cors.setAllowCredentials(true); // la cookie del refresh token
        cors.setMaxAge(3600L);
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    private static JwtAuthenticationConverter convertidorRoles() {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }
}
