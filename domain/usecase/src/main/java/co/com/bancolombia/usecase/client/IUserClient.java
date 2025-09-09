package co.com.bancolombia.usecase.client;

import co.com.bancolombia.model.client.UserClientDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IUserClient {

    Mono<UserClientDetails> findByEmail(String email);

    Flux<UserClientDetails> findByIds(List<Long> ids);
}
