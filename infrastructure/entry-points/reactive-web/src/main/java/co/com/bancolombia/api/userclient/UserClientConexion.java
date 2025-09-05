package co.com.bancolombia.api.userclient;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.exceptions.BadRequestException;
import co.com.bancolombia.model.exceptions.ForbiddenRoleException;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.usecase.client.IUserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import co.com.bancolombia.model.exceptions.UnauthorizedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserClientConexion implements IUserClient {


    private final WebClient webClient;

    public UserClientConexion(WebClient.Builder webClientBuilder,
                              @Value("${services.auth.url}") String userServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(userServiceUrl)
                .filter((request, next) ->
                        ReactiveSecurityContextHolder.getContext()
                                .map(SecurityContext::getAuthentication)
                                .cast(JwtAuthenticationToken.class)
                                .map(JwtAuthenticationToken::getToken)
                                .map(jwt -> ClientRequest.from(request)
                                        .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                                        .build()
                                )
                                .defaultIfEmpty(request)
                                .flatMap(next::exchange)
                )
                .build();
    }

    @Override
    public Mono<UserClientDetails> findByEmail(String email) {
        return webClient.get()
                .uri("/api/v1/usuarios/email/{email}", email)
                .retrieve()
                .onStatus(s -> s.value() == 401,
                        resp -> Mono.error(new UnauthorizedException("No autorizado")))
                .onStatus(s -> s.value() == 403,
                        resp -> Mono.error(new ForbiddenRoleException("No autorizado")))
                .onStatus(s -> s.value() == 404,
                        resp -> Mono.error(new NotFoundException("El usuario no existe")))
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Solicitud inválida")
                                .flatMap(body -> Mono.error(new BadRequestException(body))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Error del servicio externo")
                                .flatMap(body -> Mono.error(new RuntimeException(body))))
                .bodyToMono(UserClientDetails.class);
    }
}
