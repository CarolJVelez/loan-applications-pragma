package co.com.bancolombia.usecase;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.notifications.MessageSQS;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import co.com.bancolombia.usecase.validation.LoanValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationCaseTest {

    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private LoanValidation validation;
    @Mock private LoggerRepository logger;
    @Mock private IUserClient iUserClient;
    @Mock private LoanNotificationRepository loanNotificationRepository;

    @InjectMocks
    private LoanApplicationCase useCase;

    private LoanApplication toCreate;
    private UserClientDetails user;
    private LoanType loanType;

    @BeforeEach
    void setUp() {
        toCreate = LoanApplication.builder()
                .loanType("HIPOTECARIO")
                .email("carol@example.com")
                .amount(new BigInteger("5000000"))
                .loanTermMonths(12) // evita NPE en create()
                .build();

        user = UserClientDetails.builder()
                .userId(123L)
                .document("CC-9999")
                .name("Carol")
                .lastName("Velez")
                .maxIndebtedness(new BigDecimal("2800000"))
                .baseSalary(new BigInteger("8000000"))
                .build();

        loanType = LoanType.builder()
                .name("HIPOTECARIO")
                .interestRate(new BigDecimal("0.019"))
                .minimumAmount(BigInteger.valueOf(1_000_000))
                .maximumAmount(BigInteger.valueOf(10_000_000))
                .automaticValidation(Boolean.TRUE)
                .build();
    }

    private LoanApplication base(Long id, String email) {
        return LoanApplication.builder()
                .loanApplicationId(id)
                .email(email)
                .status("PENDING_REVIEW")
                .amount(BigInteger.valueOf(21_000_000))
                .loanTermMonths(12)
                .interestRate(new BigDecimal("3.25"))
                .build();
    }

    // ------- create(): éxito -------
    @Test
    void create_whenValid_shouldSetFieldsSaveAndLog() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class)))
                .thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));

        // usado en create()
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L))
                .thenReturn(Mono.just(0));

        // simular que el repo asigna ID para usarlo en el CapacityRequest
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication in = inv.getArgument(0);
                    return Mono.just(in.toBuilder().loanApplicationId(1L).build());
                });

        // por automaticValidation=TRUE, se envía a la lambda: hay que stubbearlo
        when(loanNotificationRepository.sendCapacityValidationRequest(any()))
                .thenReturn(Mono.just("msg-xyz"));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertEquals(new BigDecimal("0.019"), saved.getInterestRate());
                    assertNotNull(saved.getCreatedAt());
                    assertTrue(saved.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(2)));
                    assertEquals(new BigInteger("5000000"), saved.getAmount());
                    assertEquals(1L, saved.getLoanApplicationId()); // id asignado
                })
                .verifyComplete();

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository, loanNotificationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));
        inOrder.verify(loanNotificationRepository).sendCapacityValidationRequest(any());

        // hay logs adicionales en el flujo; no uses verifyNoMoreInteractions(logger)
        verify(logger, atLeastOnce()).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger, atLeastOnce()).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
    }


    @Test
    void create_whenHasPendingLoan_shouldStopAndReturnError() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.error(new RuntimeException("Loan is pending review")));
        // estos Monos no se usarán, pero así evitamos NPE al armar la cadena
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.never());
        when(validation.validateAndGetLoanType(anyString(), any())).thenReturn(Mono.never());

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Loan is pending review")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verifyNoInteractions(iUserClient, loanApplicationRepository, logger);
    }

    @Test
    void create_whenLoanTypeInvalid_shouldStopAndReturnError() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.error(new RuntimeException("Invalid loan type")));
        when(validation.validateAndGetLoanType(anyString(), any())).thenReturn(Mono.never());

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Invalid loan type")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verifyNoInteractions(iUserClient, loanApplicationRepository, logger);
    }

    @Test
    void create_whenUserClientFails_shouldPropagateError() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.error(new RuntimeException("user-client-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("user-client-down")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        verify(iUserClient).findByEmail("carol@example.com");
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);
    }

    @Test
    void create_whenLoanTypeAmountOutOfRange_shouldErrorAndStopBeforeUserClient() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW")).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO")).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class)))
                .thenReturn(Mono.error(new RuntimeException("amount-out-of-range")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("amount-out-of-range")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        verify(iUserClient, never()).findByEmail(anyString());
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);
    }

    @Test
    void create_whenLoanTypeAmountInRange_shouldCallValidateAndGetAndContinue() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW")).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO")).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L)).thenReturn(Mono.just(0));

        // El repo asigna ID para que el CapacityRequest tenga applicationId
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication in = inv.getArgument(0);
                    return Mono.just(in.toBuilder().loanApplicationId(1L).build());
                });

        // Como automaticValidation = TRUE, el use case llama a la cola de capacidad
        when(loanNotificationRepository.sendCapacityValidationRequest(any()))
                .thenReturn(Mono.just("msg-1"));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertEquals(new BigDecimal("0.019"), saved.getInterestRate());
                    assertNotNull(saved.getCreatedAt());
                    assertEquals(new BigInteger("5000000"), saved.getAmount());
                    assertEquals(1L, saved.getLoanApplicationId());
                })
                .verifyComplete();

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository, loanNotificationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));
        inOrder.verify(loanNotificationRepository).sendCapacityValidationRequest(any());

        // 👇 No usamos verifyNoMoreInteractions(logger) porque hay logs adicionales en el flujo
        verify(logger, atLeastOnce()).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger, atLeastOnce()).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
    }


    @Test
    void create_whenSaveFails_shouldPropagateErrorAndNotLogSuccess() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail(anyString())).thenReturn(Mono.just(user));
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L)).thenReturn(Mono.just(0));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenReturn(Mono.error(new RuntimeException("db-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("db-down")
                .verify();

        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger, never()).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }

    // ------- update(): APROBADO (envía plan) -------
    @Test
    void updateApproved_includesPlanFieldsInMessage() {
        LoanApplication incoming = LoanApplication.builder()
                .loanApplicationId(10L)
                .email("carol@example.com")
                .status("APPROVED")
                .observations("ok")
                .build();

        // El registro tal como existe en BD
        LoanApplication inDb = base(10L, "carol@example.com"); // trae amount, loanTermMonths, interestRate, status=PENDING_REVIEW

        UserClientDetails user = UserClientDetails.builder()
                .userId(123L).document("123").name("Carol").lastName("Velez")
                .maxIndebtedness(new BigDecimal("2800000"))
                .baseSalary(new BigInteger("8000000"))
                .build();

        // Cómo quedará guardado tras el update
        LoanApplication saved = inDb.toBuilder()
                .status("APPROVED")
                .observations("ok")
                .updatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")))
                .build();

        when(validation.validateExistLoan("carol@example.com", 10L)).thenReturn(Mono.just(inDb));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(saved));

        ArgumentCaptor<MessageSQS> msgCaptor = ArgumentCaptor.forClass(MessageSQS.class);
        when(loanNotificationRepository.sendMessageUpdateLoan(msgCaptor.capture())).thenReturn(Mono.just("id-123"));

        StepVerifier.create(useCase.update(incoming))
                .assertNext(out -> {
                    assertEquals("APPROVED", out.getStatus());
                    assertEquals("ok", out.getObservations());
                    assertNotNull(out.getUpdatedAt());
                })
                .verifyComplete();

        MessageSQS msg = msgCaptor.getValue();
        assertEquals("APROBADO", msg.getStatus()); // traducido
        assertEquals("carol@example.com", msg.getEmail());
        assertEquals("ok", msg.getObservations());
        assertNotNull(msg.getUpdatedAt());

        assertNotNull(msg.getAmount(), "amount debe ir en APROBADO");
        assertNotNull(msg.getLoanTermMonths(), "loanTermMonths debe ir en APROBADO");
        assertNotNull(msg.getAnnualInterestRate(), "annualInterestRate debe ir en APROBADO");

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository, loanNotificationRepository);
        inOrder.verify(validation).validateExistLoan("carol@example.com", 10L);
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));
        inOrder.verify(loanNotificationRepository).sendMessageUpdateLoan(any());

        verify(logger, atLeastOnce()).info(startsWith("Inicio actualizacion de prestamo del cliente con documento="), any());
        verify(logger, atLeastOnce()).info(startsWith("Prestamo actualizado doc="), any(), any(), any(), any());
    }

    // ------- update(): RECHAZADO (sin plan) -------
    @Test
    void updateRejected_sendsMessageWithoutPlanFields() {
        LoanApplication incoming = LoanApplication.builder()
                .loanApplicationId(11L)
                .email("carol@example.com")
                .status("REJECTED")
                .observations("no")
                .build();

        // como está en BD antes del cambio
        LoanApplication inDb = base(11L, "carol@example.com"); // tiene amount, loanTermMonths, interestRate, status=PENDING_REVIEW

        UserClientDetails user = UserClientDetails.builder()
                .userId(123L).document("123").name("Carol").lastName("Velez")
                .maxIndebtedness(new BigDecimal("2800000"))
                .baseSalary(new BigInteger("8000000")) // BigInteger
                .build();

        // cómo quedará guardado tras el update
        LoanApplication saved = inDb.toBuilder()
                .status("REJECTED")
                .observations("no")
                .updatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")))
                .build();

        when(validation.validateExistLoan("carol@example.com", 11L)).thenReturn(Mono.just(inDb));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(saved));

        ArgumentCaptor<MessageSQS> msgCaptor = ArgumentCaptor.forClass(MessageSQS.class);
        when(loanNotificationRepository.sendMessageUpdateLoan(msgCaptor.capture())).thenReturn(Mono.just("id-456"));

        StepVerifier.create(useCase.update(incoming))
                .assertNext(out -> {
                    assertEquals("REJECTED", out.getStatus());
                    assertEquals("no", out.getObservations());
                    assertNotNull(out.getUpdatedAt());
                })
                .verifyComplete();

        MessageSQS msg = msgCaptor.getValue();
        assertEquals("RECHAZADO", msg.getStatus()); // traducido
        assertEquals("carol@example.com", msg.getEmail());
        assertEquals("no", msg.getObservations());
        assertNotNull(msg.getUpdatedAt());
        // En rechazado NO deben ir campos de plan
        assertNull(msg.getAmount(), "amount NO debe ir en RECHAZADO");
        assertNull(msg.getLoanTermMonths(), "loanTermMonths NO debe ir en RECHAZADO");
        assertNull(msg.getAnnualInterestRate(), "annualInterestRate NO debe ir en RECHAZADO");

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository, loanNotificationRepository);
        inOrder.verify(validation).validateExistLoan("carol@example.com", 11L);
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));
        inOrder.verify(loanNotificationRepository).sendMessageUpdateLoan(any());

        verify(logger, atLeastOnce()).info(startsWith("Inicio actualizacion de prestamo del cliente con documento="), any());
        verify(logger, atLeastOnce()).info(startsWith("Prestamo actualizado doc="), any(), any(), any(), any());
    }

    @Test
    void create_whenAutomaticValidationDisabled_shouldNotSendCapacityRequest() {
        // LoanType sin validación automática
        LoanType manualType = loanType.toBuilder().automaticValidation(Boolean.FALSE).build();

        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW")).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO")).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(manualType));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L)).thenReturn(Mono.just(0));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication in = inv.getArgument(0);
                    return Mono.just(in.toBuilder().loanApplicationId(2L).build());
                });

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals(2L, saved.getLoanApplicationId());
                })
                .verifyComplete();

        // No se debe llamar a la cola
        verify(loanNotificationRepository, never()).sendCapacityValidationRequest(any());
    }

    @Test
    void create_whenCapacityRequestFails_shouldPropagateError() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType)); // automaticValidation=TRUE
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L)).thenReturn(Mono.just(0));

        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication in = inv.getArgument(0);
                    return Mono.just(in.toBuilder().loanApplicationId(3L).build());
                });

        when(loanNotificationRepository.sendCapacityValidationRequest(any()))
                .thenReturn(Mono.error(new RuntimeException("sqs-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("sqs-down")
                .verify();
    }


    @Test
    void create_shouldClampAvailableIndebtednessToZero_whenTotalApprovedExceedsMax() {
        // maxIndebtedness = 2_800_000; totalApproved = 3_000_000 -> available debe ser 0
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(validation.calculateTotalApprovedLoansMonthlyPayment(123L)).thenReturn(Mono.just(3_000_000));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> {
                    LoanApplication in = inv.getArgument(0);
                    assertEquals(0, in.getAvailableIndebtedness()); // clamp aquí antes de persistir
                    return Mono.just(in.toBuilder().loanApplicationId(4L).build());
                });
        when(loanNotificationRepository.sendCapacityValidationRequest(any())).thenReturn(Mono.just("msg-ok"));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> assertEquals(4L, saved.getLoanApplicationId()))
                .verifyComplete();
    }

    @Test
    void update_whenStatusUnchanged_shouldReturnWithoutSavingOrNotifying() {
        // incoming status = mismo que en BD: PENDING_REVIEW
        LoanApplication incoming = LoanApplication.builder()
                .loanApplicationId(20L)
                .email("carol@example.com")
                .status("PENDING_REVIEW")
                .observations("n/a")
                .build();

        LoanApplication inDb = base(20L, "carol@example.com"); // ya está en PENDING_REVIEW

        when(validation.validateExistLoan("carol@example.com", 20L)).thenReturn(Mono.just(inDb));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));

        StepVerifier.create(useCase.update(incoming))
                .assertNext(out -> {
                    assertEquals("PENDING_REVIEW", out.getStatus()); // igual que estaba
                    // updatedAt no debería cambiar si devuelves tal cual loanBd (tu código hace Mono.just(loanBd))
                    assertEquals(inDb.getUpdatedAt(), out.getUpdatedAt());
                })
                .verifyComplete();

        verify(loanApplicationRepository, never()).save(any());
        verify(loanNotificationRepository, never()).sendMessageUpdateLoan(any());
    }

    @Test
    void update_whenStatusIsNeitherApprovedNorRejected_shouldNotNotify() {
        LoanApplication incoming = LoanApplication.builder()
                .loanApplicationId(21L)
                .email("carol@example.com")
                .status("PENDING_REVIEW") // diferente al de BD (forzamos que cambie a PENDING_REVIEW)
                .observations("checking")
                .build();

        // En BD está en otro estado para que sí se haga save
        LoanApplication inDb = base(21L, "carol@example.com").toBuilder()
                .status("REJECTED")
                .build();

        LoanApplication saved = inDb.toBuilder()
                .status("PENDING_REVIEW")
                .observations("checking")
                .updatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")))
                .build();

        when(validation.validateExistLoan("carol@example.com", 21L)).thenReturn(Mono.just(inDb));
        when(iUserClient.findByEmail("carol@example.com")).thenReturn(Mono.just(user));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.update(incoming))
                .assertNext(out -> assertEquals("PENDING_REVIEW", out.getStatus()))
                .verifyComplete();

        verify(loanNotificationRepository, never()).sendMessageUpdateLoan(any());
    }

}
