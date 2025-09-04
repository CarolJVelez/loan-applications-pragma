package co.com.bancolombia.usecase.loanApplication;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.validation.LoanValidation;
import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class LoanApplicationCase {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanValidation validation;
    private final LoggerRepository logger;
    private final IUserClient iUserClient;

    public Mono<LoanApplication> create(LoanApplication loanApplication) {
        final String PENDING = "PENDING_REVIEW";
        final String loanTypeName = loanApplication.getLoanType();
        final String email = loanApplication.getEmail();

        return validation.validateNoPendingLoan(email, PENDING)
                .then(validation.validateLoanTypeExists(loanTypeName))
                .then(validation.validateLoanType(loanTypeName, loanApplication.getAmount()))
                .then(iUserClient.findByEmail(email))
                .flatMap(userClientDetails -> {
                    logger.info("Inicio creacion de prestamo del cliente con documento= {}", userClientDetails.getDocument());

                    loanApplication.setStatus(PENDING);
                    loanApplication.setDocument(userClientDetails.getDocument());
                    loanApplication.setUserId(userClientDetails.getUserId());
                    loanApplication.setCreatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")));

                    return loanApplicationRepository.save(loanApplication);
                })
                .doOnSuccess(u -> logger.info("Prestamo creado doc={}, valor={}, estado={}",
                        u.getDocument(), u.getAmount(), u.getStatus()));
    }

}