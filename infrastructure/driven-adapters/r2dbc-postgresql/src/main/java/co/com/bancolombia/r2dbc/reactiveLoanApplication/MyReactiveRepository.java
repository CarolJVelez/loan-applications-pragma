package co.com.bancolombia.r2dbc.reactiveLoanApplication;

import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface MyReactiveRepository extends ReactiveCrudRepository<LoanApplicationEntity, Long>, ReactiveQueryByExampleExecutor<LoanApplicationEntity> {

    Mono<Boolean> existsByEmailAndStatus(String email, String status);

    @Query("SELECT * FROM loan_application " +
            "WHERE status = ANY(:statuses) " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<LoanApplicationEntity> findForStatuses(@Param("statuses") String[] statuses,
                                                @Param("limit") long limit,
                                                @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM loan_application " +
            "WHERE status = ANY(:statuses)")
    Mono<Long> countForStatuses(@Param("statuses") String[] statuses);

    Mono<LoanApplicationEntity> findByEmailAndLoanApplicationId(String email, Long loanApplicationId);
}
