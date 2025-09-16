package co.com.bancolombia.usecase.loanApplication;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.notifications.CapacityRequest;
import co.com.bancolombia.model.notifications.MessageSQS;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.validation.LoanValidation;
import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class LoanApplicationCase {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanValidation validation;
    private final LoggerRepository logger;
    private final IUserClient iUserClient;
    private final LoanNotificationRepository loanNotificationRepository;

    public Mono<LoanApplication> create(LoanApplication loanApplication) {
        final String PENDING = "PENDING_REVIEW";
        final String loanTypeName = loanApplication.getLoanType();
        final String email = loanApplication.getEmail();

        return validation.validateNoPendingLoan(email, PENDING)
                .then(validation.validateLoanTypeExists(loanTypeName))
                .then(validation.validateAndGetLoanType(loanTypeName, loanApplication.getAmount()))
                .zipWhen(loanType -> iUserClient.findByEmail(email))
                .flatMap(tuple -> {
                    LoanType loanType = tuple.getT1();
                    UserClientDetails userClientDetails = tuple.getT2();

                    logger.info("Inicio creacion de prestamo del cliente con documento= {}", userClientDetails.getDocument());

                    int currentPayment = validation.calculateCurrentLoanMonthlyPayment(loanApplication.getAmount(), loanType.getInterestRate(), loanApplication.getLoanTermMonths());

                    return validation.calculateTotalApprovedLoansMonthlyPayment(userClientDetails.getUserId())
                            .map(totalApproved -> {
                                BigDecimal totalApprovedBD = BigDecimal.valueOf(totalApproved);
                                BigDecimal available = userClientDetails.getMaxIndebtedness().subtract(totalApprovedBD);

                                if (available.compareTo(BigDecimal.ZERO) < 0) {
                                    available = BigDecimal.ZERO;
                                }
                                return loanApplication.toBuilder()
                                        .status(PENDING)
                                        .document(userClientDetails.getDocument())
                                        .userId(userClientDetails.getUserId())
                                        .names(userClientDetails.getName())
                                        .createdAt(OffsetDateTime.now(ZoneId.of("America/Bogota")))
                                        .interestRate(loanType.getInterestRate())
                                        .currentLoanMonthlyPayment(currentPayment)
                                        .totalApprovedLoansMonthlyPayment(totalApproved)
                                        .availableIndebtedness(available.intValue())
                                        .maxIndebtedness(userClientDetails.getMaxIndebtedness())
                                        .build();
                            })
                            .flatMap(loanApplicationRepository::save)
                            .flatMap(saved -> {
                                if (Boolean.TRUE.equals(loanType.getAutomaticValidation())) {
                                    logger.info("Entre a enviar el mensaje a la lambda carolv");
                                    CapacityRequest req = CapacityRequest.builder()
                                            .applicationId(saved.getLoanApplicationId())
                                            .email(saved.getEmail())
                                            .loanType(saved.getLoanType())
                                            .amount(saved.getAmount().doubleValue())
                                            .loanTermMonths(saved.getLoanTermMonths())
                                            .annualInterestRate(loanType.getInterestRate().doubleValue())
                                            .customerId(String.valueOf(userClientDetails.getUserId()))
                                            .maxIndebtedness(userClientDetails.getMaxIndebtedness().doubleValue())
                                            .currentMonthlySalary(userClientDetails.getBaseSalary()!=null?userClientDetails.getBaseSalary().doubleValue():0.0)
                                            .currentLoanMonthlyPayment(saved.getCurrentLoanMonthlyPayment())
                                            .totalApprovedLoansMonthlyPayment(saved.getTotalApprovedLoansMonthlyPayment())
                                            .build();
                                    logger.info("Entre a enviar el mensaje a la lambda carolv2 ");
                                    return loanNotificationRepository.sendCapacityValidationRequest(req)
                                            .doOnSuccess(id -> logger.info("[CREATE] CapacityRequest enviado msgId={}", id))
                                            .thenReturn(saved);
                                }
                                return Mono.just(saved);
                            });
                })
                .doOnSuccess(u -> logger.info("Prestamo creado doc={}, valor={}, estado={}, id={}",
                        u.getDocument(), u.getAmount(), u.getStatus(), u.getLoanApplicationId()));
    }

    public Mono<LoanApplication> update(LoanApplication loanApplication) {
        final String email = loanApplication.getEmail();
        final Long id = loanApplication.getLoanApplicationId();
        final String newStatus = loanApplication.getStatus();

        return validation.validateExistLoan(email, id)
                .zipWhen(ignored -> iUserClient.findByEmail(email))
                .flatMap(tuple -> {
                    LoanApplication loanBd = tuple.getT1();
                    UserClientDetails userClientDetails = tuple.getT2();

                    logger.info("Inicio actualizacion de prestamo del cliente con documento= {}", userClientDetails.getDocument());

                    if(loanBd.getStatus().equals(newStatus)){
                        logger.info("El préstamo ya está en estado={}  id={}", newStatus, loanBd.getLoanApplicationId());
                        return Mono.just(loanBd);
                    }
                    loanBd.setStatus(newStatus);
                    loanBd.setObservations(loanApplication.getObservations());
                    loanBd.setUpdatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")));

                    return loanApplicationRepository.save(loanBd)
                            .flatMap(saved -> {
                                if ("APPROVED".equalsIgnoreCase(saved.getStatus()) || "REJECTED".equalsIgnoreCase(saved.getStatus())) {
                                    String fullName =
                                            (userClientDetails.getName() != null ? userClientDetails.getName() : "")
                                                    + (userClientDetails.getLastName() != null ? " " + userClientDetails.getLastName() : "");

                                    MessageSQS msg = MessageSQS.builder()
                                            .fullName(fullName.trim())
                                            .status(saved.getStatus().equals("APPROVED") ? "APROBADO" : "RECHAZADO")
                                            .email(saved.getEmail())
                                            .observations(saved.getObservations())
                                            .updatedAt(saved.getUpdatedAt())
                                            .amount("APPROVED".equalsIgnoreCase(saved.getStatus()) ? saved.getAmount() : null)
                                            .loanTermMonths("APPROVED".equalsIgnoreCase(saved.getStatus()) ? saved.getLoanTermMonths() : null)
                                            .annualInterestRate("APPROVED".equalsIgnoreCase(saved.getStatus()) ? saved.getInterestRate() : null)
                                            .build();
                                    logger.info("ENtre al if carolv3"+ msg);
                                    return loanNotificationRepository.sendMessageUpdateLoan(msg)
                                            .doOnSuccess(msgId -> logger.info("SQS enviado messageId={} solicitudId={}", msgId, saved.getLoanApplicationId()))
                                            .thenReturn(saved);
                                }
                                return Mono.just(saved);
                            });
                })
                .doOnSuccess(u -> logger.info("Prestamo actualizado doc={}, valor={}, estado={}, id={}",
                        u.getDocument(), u.getAmount(), u.getStatus(), u.getLoanApplicationId()));
    }

}