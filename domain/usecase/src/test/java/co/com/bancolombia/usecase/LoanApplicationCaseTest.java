package co.com.bancolombia.usecase;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import co.com.bancolombia.usecase.validation.LoanValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationCaseTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanValidation validation;

    @Mock
    private LoggerRepository logger;

    @Mock
    private IUserClient iUserClient;

    @InjectMocks
    private LoanApplicationCase useCase;

    private LoanApplication toCreate;
    private UserClientDetails user;

    @BeforeEach
    void setUp() {
        toCreate = LoanApplication.builder()
                .loanType("HIPOTECARIO")
                .email("carol@example.com")
                .amount(new BigInteger("5000000"))
                .build();

        user = UserClientDetails.builder()
                .userId(123L)
                .document("CC-9999")
                .build();
    }

    // ------- create(): éxito -------
    @Test
    void create_whenValid_shouldSetFieldsSaveAndLog() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.empty());
        // NUEVO: stub para que no devuelva null y evitar NPE en then(...)
        when(validation.validateLoanType(eq("HIPOTECARIO"), any()))
                .thenReturn(Mono.empty());

        when(iUserClient.findByEmail("carol@example.com"))
                .thenReturn(Mono.just(user));

        // Al guardar, validamos que ya venga con status/doc/userId/createdAt
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertNotNull(saved.getCreatedAt());
                    // createdAt ~ ahora
                    assertTrue(saved.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(2)));
                    // amount es BigInteger
                    assertEquals(new BigInteger("5000000"), saved.getAmount());
                })
                .verifyComplete();

        // Orden: noPending -> typeExists -> typeRange -> findByEmail -> save
        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateLoanType(eq("HIPOTECARIO"), any());
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));

        // Logs: inicio y éxito
        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger).info(startsWith("Prestamo creado doc="), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void create_whenHasPendingLoan_shouldStopAndReturnError() {
        // Simula que el préstamo tiene un estado "PENDING_REVIEW"
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.error(new RuntimeException("Loan is pending review")));

        // Se ejecuta el flujo y se espera un error con el mensaje esperado
        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Loan is pending review")  // Espera el mensaje de error
                .verify();

        // Verifica que no se realicen validaciones ni interacciones adicionales
        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation, never()).validateLoanTypeExists(anyString());  // El flujo no llegó a esta validación
        verify(validation, never()).validateLoanType(anyString(), any());
        verify(iUserClient, never()).findByEmail(anyString());
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);  // No debería haber interacción con el logger
    }

    @Test
    void create_whenLoanTypeInvalid_shouldStopAndReturnError() {
        // El préstamo no tiene pendiente, pero el tipo de préstamo es inválido
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.error(new RuntimeException("Invalid loan type")));

        // Se ejecuta el flujo y se espera un error con el mensaje esperado
        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Invalid loan type")
                .verify();

        // Verifica que se haya realizado la primera validación y luego se haya detenido
        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verify(validation, never()).validateLoanType(anyString(), any());  // El flujo se detuvo antes
        verify(iUserClient, never()).findByEmail(anyString());
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);
    }

    // ------- create(): fallo al consultar usuario -------
    @Test
    void create_whenUserClientFails_shouldPropagateError() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        // NUEVO: pasa validación de rango para alcanzar el fallo de user client
        when(validation.validateLoanType(anyString(), any())).thenReturn(Mono.empty());

        when(iUserClient.findByEmail("carol@example.com"))
                .thenReturn(Mono.error(new RuntimeException("user-client-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("user-client-down")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verify(validation).validateLoanType(eq("HIPOTECARIO"), any());
        verify(iUserClient).findByEmail("carol@example.com");
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);
    }

    // ------- create(): fallo al guardar -------
    @Test
    void create_whenSaveFails_shouldPropagateErrorAndNotLogSuccess() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        // NUEVO: pasa validación de rango para alcanzar el fallo de save
        when(validation.validateLoanType(anyString(), any())).thenReturn(Mono.empty());
        when(iUserClient.findByEmail(anyString())).thenReturn(Mono.just(user));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenReturn(Mono.error(new RuntimeException("db-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("db-down")
                .verify();

        // Se loguea el inicio (en el flatMap), pero no el éxito
        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger, never()).info(startsWith("Prestamo creado doc="), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }

    // ------- create(): error por validateLoanType (fuera de rango) -------
    @Test
    void create_whenLoanTypeAmountOutOfRange_shouldErrorAndStopBeforeUserClient() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.empty());
        // Forzamos error por rango (usar any() cubre incluso nulos)
        when(validation.validateLoanType(eq("HIPOTECARIO"), any()))
                .thenReturn(Mono.error(new RuntimeException("amount-out-of-range")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("amount-out-of-range")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        verify(validation).validateLoanType(eq("HIPOTECARIO"), any());
        verify(iUserClient, never()).findByEmail(anyString());
        verify(loanApplicationRepository, never()).save(any());
        verifyNoInteractions(logger);
    }

    // ÉXITO con validateLoanType (dentro de rango)
    @Test
    void create_whenLoanTypeAmountInRange_shouldCallValidateLoanTypeAndContinue() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.empty());
        // <- nueva validación: pasa porque está en rango
        when(validation.validateLoanType(eq("HIPOTECARIO"), any(BigInteger.class)))
                .thenReturn(Mono.empty());

        when(iUserClient.findByEmail("carol@example.com"))
                .thenReturn(Mono.just(user));

        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertNotNull(saved.getCreatedAt());
                })
                .verifyComplete();

        // Verifica el ORDEN incluyendo la nueva validación
        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));

        // Logs de creación (se ejecutan al final)
        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger).info(startsWith("Prestamo creado doc="), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }

}