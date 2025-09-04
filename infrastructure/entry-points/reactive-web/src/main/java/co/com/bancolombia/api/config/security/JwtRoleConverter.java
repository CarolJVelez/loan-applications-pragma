package co.com.bancolombia.api.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class JwtRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {
    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        Collection<? extends GrantedAuthority> authorities =
                role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of();
        return Mono.just(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
    }
}
