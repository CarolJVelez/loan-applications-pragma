package co.com.bancolombia.api.userclient;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.usecase.client.IUserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserClientConexion implements IUserClient {


    private final WebClient webClient;

    public UserClientConexion(WebClient.Builder webClientBuilder, @Value("${services.auth.url}") String userServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(userServiceUrl).build();
    }

    @Override
    public Mono<UserClientDetails> findByEmail(String email) {
        return webClient.get()
                .uri("/api/v1/usuarios/email/{email}", email)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response ->Mono.error(new NotFoundException("El usuario no existe")))
                .bodyToMono(UserClientDetails.class);
    }
}
