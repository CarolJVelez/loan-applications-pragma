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
/*
    public Mono<LoanApplication> update(Long id, LoanApplication LoanApplication) {
        return loanApplicationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado: " + id)))
                .flatMap(ex -> loanApplicationRepository.update(
                        ex.toBuilder()
                                .name(LoanApplication.getName())
                                .lastname(LoanApplication.getLastname())
                                .birthDate(LoanApplication.getBirthDate())
                                .address(LoanApplication.getAddress())
                                .phone(LoanApplication.getPhone())
                                .email(LoanApplication.getEmail())
                                .baseSalary(LoanApplication.getBaseSalary())
                                .documentId(LoanApplication.getDocumentId())
                                .build()
                ));
    }

    public Mono<LoanApplication> findById(Long id) {
        return loanApplicationRepository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado: " + id)));
    }

    public Flux<LoanApplication> findAll() {
        return loanApplicationRepository.findAll();
    }

    public Mono<Void> delete(Long id) {
        return loanApplicationRepository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Usuario no encontrado: " + id))).then(loanApplicationRepository.deleteById(id));
    }*/
}