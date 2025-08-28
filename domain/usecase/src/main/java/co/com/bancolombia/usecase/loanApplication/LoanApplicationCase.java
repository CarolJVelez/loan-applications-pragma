package co.com.bancolombia.usecase.loanApplication;

import co.com.bancolombia.model.exceptions.*;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanType.gateways.LoanTypeRepository;
import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RequiredArgsConstructor
public class LoanApplicationCase {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoggerRepository logger;

    public Mono<LoanApplication> create(LoanApplication loanApplication) {
        logger.info("Inicio creacion de prestamo del cliente con documento= {}", loanApplication.getDocument());
        loanApplication.setStatus("PENDING_REVIEW");
        var now = OffsetDateTime.now(ZoneId.of("America/Bogota"));
        loanApplication.setCreatedAt(now);
        return loanApplicationRepository
                .existsByDocumentAndStatus(loanApplication.getDocument(), "PENDING_REVIEW")
                .flatMap(existsPending -> {
                    if (Boolean.TRUE.equals(existsPending)) {
                        logger.info("Tienes un prestamo en estado pendiente: {}", loanApplication.getDocument());
                        return Mono.error(new LoanPendingException(loanApplication.getDocument()));
                    }
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