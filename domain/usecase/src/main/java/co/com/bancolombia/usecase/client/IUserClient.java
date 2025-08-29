package co.com.bancolombia.usecase.client;

import co.com.bancolombia.model.client.UserClientDetails;
import reactor.core.publisher.Mono;

public interface IUserClient {

    Mono<UserClientDetails> findByEmail(String email);
}
