package co.com.bancolombia.api.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, JwtRoleConverter jwtRoleConverter) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/solicitud").hasRole("CLIENTE")
                        .anyExchange().authenticated()
                )
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter))
            )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> { // 401
                            var res = exchange.getResponse();
                            res.setStatusCode(HttpStatus.UNAUTHORIZED);
                            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            var json = """
                        {"status":401,"message":"Token inválido o ausente."}
                        """;
                            var buf = res.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
                            return res.writeWith(Mono.just(buf));
                        })
                        .accessDeniedHandler((exchange, ex) -> {      // 403
                            var res = exchange.getResponse();
                            res.setStatusCode(HttpStatus.FORBIDDEN);
                            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            var json = """
                        {"status":403,"message":"No autorizado."}
                        """;
                            var buf = res.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
                            return res.writeWith(Mono.just(buf));
                        })
                )
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] key = secret.getBytes();
        return NimbusReactiveJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
    }
}
