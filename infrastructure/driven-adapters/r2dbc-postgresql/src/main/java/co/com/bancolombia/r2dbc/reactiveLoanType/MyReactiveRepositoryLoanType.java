package co.com.bancolombia.r2dbc.reactiveLoanType;

import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.r2dbc.entities.LoanTypeEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MyReactiveRepositoryLoanType extends ReactiveCrudRepository<LoanTypeEntity, Long>, ReactiveQueryByExampleExecutor<LoanTypeEntity> {

    Mono<Boolean> existsByName(String name);

    Mono<LoanType> findByName(String name);
}
