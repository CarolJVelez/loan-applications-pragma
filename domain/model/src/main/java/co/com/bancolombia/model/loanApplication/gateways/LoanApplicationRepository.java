package co.com.bancolombia.model.loanApplication.gateways;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanApplicationRepository {

    Mono<LoanApplication> save(LoanApplication u);

    Mono<Boolean> existsByDocumentAndStatus(String document, String status);


}
