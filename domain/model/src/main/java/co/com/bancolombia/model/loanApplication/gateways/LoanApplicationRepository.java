package co.com.bancolombia.model.loanApplication.gateways;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import reactor.core.publisher.Mono;

public interface LoanApplicationRepository {

    Mono<LoanApplication> save(LoanApplication u);

    Mono<Boolean> existsByEmailAndStatus(String email, String status);


}
