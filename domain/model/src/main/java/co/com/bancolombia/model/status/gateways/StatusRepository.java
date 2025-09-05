package co.com.bancolombia.model.status.gateways;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface StatusRepository {
    Mono<Boolean> existsByName(String name);
    //Flux<String> findExistingNamesIn(Collection<String> names);
}
