package co.com.bancolombia.model.loanType.gateways;

import co.com.bancolombia.model.loanType.LoanType;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {

    Mono<Boolean> existsByName(String name);

    Mono<LoanType> existsByNameForAmount(String name);


}
