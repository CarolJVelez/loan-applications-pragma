package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class HandlerLoanApplication {

    private final LoanApplicationCase loanApplicationCase;
    private final LoanApplicationMapper mapper;
    private final LoggerRepository logger;

    public Mono<LoanApplication> createUser(CreateLoanApplicationDTO dto) {
        logger.info("POST /api/v1/solicitud recibido");
        return Mono.just(dto)
                .map(mapper::toModel)
                .flatMap(loanApplicationCase::create)
                .doOnError(e -> logger.error("Error en createUser: {}", e.getMessage()));
    }
/*
    public Flux<LoanApplication> listAllUsers()
    {
        logger.info("GET /api/v1/solicitud");
        return loanApplicationCase.findAll();
    }

    public Mono<LoanApplication> findUserById(Long id)
    {
        logger.info("GET /api/v1/solicitud/{id}");
        return loanApplicationCase.findById(id);
    }*/
}