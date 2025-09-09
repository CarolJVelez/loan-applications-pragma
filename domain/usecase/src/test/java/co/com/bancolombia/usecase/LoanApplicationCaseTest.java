package co.com.bancolombia.usecase;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanType.LoanType;
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
    private LoanType loanType;

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
                .name("Carol")
                .build();

        loanType = LoanType.builder()
                .name("HIPOTECARIO")
                .interestRate(new BigDecimal("0.019"))
                .minimumAmount(BigInteger.valueOf(1_000_000))
                .maximumAmount(BigInteger.valueOf(10_000_000))
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
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertEquals(new BigDecimal("0.019"), saved.getInterestRate());
                    assertNotNull(saved.getCreatedAt());
                    assertTrue(saved.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(2)));
                    assertEquals(new BigInteger("5000000"), saved.getAmount());
                })
                .verifyComplete();

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));

        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        // 4 argumentos: doc, valor, estado, id
        verify(logger).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }


    @Test
    void create_whenHasPendingLoan_shouldStopAndReturnError() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.error(new RuntimeException("Loan is pending review")));
        // Evita NPE en el encadenamiento .then(...): estos Monos se crean en tiempo de ensamblaje
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.never());
        when(validation.validateAndGetLoanType(anyString(), any())).thenReturn(Mono.never());

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Loan is pending review")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        // OJO: NO verificar "never" en las siguientes porque se invocan al ensamblar la cadena
        verifyNoInteractions(iUserClient, loanApplicationRepository);
        verifyNoInteractions(logger);
    }



    @Test
    void create_whenLoanTypeInvalid_shouldStopAndReturnError() {
        when(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists("HIPOTECARIO"))
                .thenReturn(Mono.error(new RuntimeException("Invalid loan type")));
        // Evita NPE en el siguiente .then(...): se crea el Mono al ensamblar
        when(validation.validateAndGetLoanType(anyString(), any())).thenReturn(Mono.never());

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("Invalid loan type")
                .verify();

        verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        verify(validation).validateLoanTypeExists("HIPOTECARIO");
        // NO verificar "never()" sobre validateAndGetLoanType: se invoca al ensamblar
        verifyNoInteractions(iUserClient, loanApplicationRepository);
        verifyNoInteractions(logger);
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
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.create(toCreate))
                .assertNext(saved -> {
                    assertEquals("PENDING_REVIEW", saved.getStatus());
                    assertEquals("CC-9999", saved.getDocument());
                    assertEquals(123L, saved.getUserId());
                    assertEquals(new BigDecimal("0.019"), saved.getInterestRate());
                    assertNotNull(saved.getCreatedAt());
                })
                .verifyComplete();

        InOrder inOrder = inOrder(validation, iUserClient, loanApplicationRepository);
        inOrder.verify(validation).validateNoPendingLoan("carol@example.com", "PENDING_REVIEW");
        inOrder.verify(validation).validateLoanTypeExists("HIPOTECARIO");
        inOrder.verify(validation).validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class));
        inOrder.verify(iUserClient).findByEmail("carol@example.com");
        inOrder.verify(loanApplicationRepository).save(any(LoanApplication.class));

        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void create_whenSaveFails_shouldPropagateErrorAndNotLogSuccess() {
        when(validation.validateNoPendingLoan(anyString(), anyString())).thenReturn(Mono.empty());
        when(validation.validateLoanTypeExists(anyString())).thenReturn(Mono.empty());
        when(validation.validateAndGetLoanType(eq("HIPOTECARIO"), any(BigInteger.class))).thenReturn(Mono.just(loanType));
        when(iUserClient.findByEmail(anyString())).thenReturn(Mono.just(user));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenReturn(Mono.error(new RuntimeException("db-down")));

        StepVerifier.create(useCase.create(toCreate))
                .expectErrorMessage("db-down")
                .verify();

        verify(logger).info(startsWith("Inicio creacion de prestamo del cliente con documento="), any());
        verify(logger, never()).info(startsWith("Prestamo creado doc="), any(), any(), any(), any());
        verifyNoMoreInteractions(logger);
    }
}
