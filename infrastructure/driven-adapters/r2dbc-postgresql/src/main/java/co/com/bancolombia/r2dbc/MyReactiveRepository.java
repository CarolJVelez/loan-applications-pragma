package co.com.bancolombia.r2dbc;

import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

// TODO: This file is just an example, you should delete or modify it
public interface MyReactiveRepository extends ReactiveCrudRepository<LoanApplicationEntity, Long>, ReactiveQueryByExampleExecutor<LoanApplicationEntity> {

    //Mono<Void> deleteByUserId(Long userId);

    Mono<Boolean> existsByDocumentAndStatus(String document, String status);
}
