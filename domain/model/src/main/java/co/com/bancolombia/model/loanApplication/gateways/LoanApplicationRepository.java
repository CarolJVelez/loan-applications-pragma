package co.com.bancolombia.model.loanApplication.gateways;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface LoanApplicationRepository {

    Mono<LoanApplication> save(LoanApplication u);

    Mono<Boolean> existsByEmailAndStatus(String email, String status);

    Flux<LoanApplication> findByStatuses(Collection<String> statuses, int page, int size);

    Mono<Long> countByStatuses(Collection<String> statuses);

    Mono<LoanApplication> findByEmailAndId(String email, Long id);



}
