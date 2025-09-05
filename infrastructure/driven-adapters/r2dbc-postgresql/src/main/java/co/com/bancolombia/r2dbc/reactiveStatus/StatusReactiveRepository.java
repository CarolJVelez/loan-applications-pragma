package co.com.bancolombia.r2dbc.reactiveStatus;

import co.com.bancolombia.r2dbc.entities.LoanStatusEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface StatusReactiveRepository extends ReactiveCrudRepository<LoanStatusEntity, Long>, ReactiveQueryByExampleExecutor<LoanStatusEntity> {
    Mono<Boolean> existsByName(String name);

/*    Flux<String> findExistingNamesIn(Collection<String> names);*/
}
