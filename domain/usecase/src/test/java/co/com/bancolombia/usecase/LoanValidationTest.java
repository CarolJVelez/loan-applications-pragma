package co.com.bancolombia.usecase;

import co.com.bancolombia.model.exceptions.BadRequestException;
import co.com.bancolombia.model.exceptions.LoanPendingException;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.loanType.gateways.LoanTypeRepository;
import co.com.bancolombia.usecase.validation.LoanValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanValidationTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private LoggerRepository logger;

    private LoanValidation validation;

    @BeforeEach
    void setUp() {
        validation = new LoanValidation(loanApplicationRepository, loanTypeRepository, logger);
    }

    // ---- validateNoPendingLoan: sin pendiente -> OK ----
    @Test
    void validateNoPendingLoan_whenNoPending_shouldComplete() {
        when(loanApplicationRepository.existsByEmailAndStatus("carol@example.com", "PENDING_REVIEW"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .verifyComplete();

        verify(loanApplicationRepository).existsByEmailAndStatus("carol@example.com", "PENDING_REVIEW");
        verifyNoMoreInteractions(loanApplicationRepository);
        verifyNoInteractions(logger);
    }

    // ---- validateNoPendingLoan: hay pendiente -> error y log ----
    @Test
    void validateNoPendingLoan_whenHasPending_shouldErrorAndLog() {
        when(loanApplicationRepository.existsByEmailAndStatus(anyString(), anyString()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(validation.validateNoPendingLoan("carol@example.com", "PENDING_REVIEW"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof LoanPendingException);
                    assertTrue(err.getMessage().contains("carol@example.com"));
                })
                .verify();

        verify(loanApplicationRepository).existsByEmailAndStatus("carol@example.com", "PENDING_REVIEW");
        verify(logger).info("Tienes un prestamo en estado pendiente");
        verifyNoMoreInteractions(logger);
    }

    // ---- validateLoanTypeExists: (según tu código) exist==true -> error ----
    @Test
    void validateLoanTypeExists_whenExists_shouldErrorAndLog() {
        when(loanTypeRepository.existsByName("HIPOTECARIO")).thenReturn(Mono.just(true));

        StepVerifier.create(validation.validateLoanTypeExists("HIPOTECARIO"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof NotFoundException);
                    assertTrue(err.getMessage().contains("Tipo de prestamo no valido: HIPOTECARIO"));
                })
                .verify();

        verify(loanTypeRepository).existsByName("HIPOTECARIO");
        verify(logger).info("Tipo de prestamo no valido: HIPOTECARIO");
        verifyNoMoreInteractions(logger);
    }

    // ---- validateLoanTypeExists: no existe -> OK ----
    @Test
    void validateLoanTypeExists_whenNotExists_shouldComplete() {
        when(loanTypeRepository.existsByName("LIBRE_INVERSION")).thenReturn(Mono.just(false));

        StepVerifier.create(validation.validateLoanTypeExists("LIBRE_INVERSION"))
                .verifyComplete();

        verify(loanTypeRepository).existsByName("LIBRE_INVERSION");
        verifyNoInteractions(logger);
    }

    private LoanType loanType(BigInteger min, BigInteger max) {
        // Si usas Lombok builder:
        return LoanType.builder()
                .minimumAmount(min)
                .maximumAmount(max)
                .build();
        // Si no tienes builder, crea el objeto como corresponda en tu proyecto.
    }

    // OK: monto dentro del rango [min, max]
    @Test
    void validateLoanType_whenAmountInRange_shouldComplete() {
        when(loanTypeRepository.existsByNameForAmount("HIPOTECARIO"))
                .thenReturn(Mono.just(loanType(BigInteger.valueOf(1_000_000), BigInteger.valueOf(10_000_000))));

        StepVerifier.create(validation.validateLoanType("HIPOTECARIO", BigInteger.valueOf(5_000_000)))
                .verifyComplete();

        verify(loanTypeRepository).existsByNameForAmount("HIPOTECARIO");
        verifyNoInteractions(logger);
    }

    // ERROR: monto MENOR al mínimo
    @Test
    void validateLoanType_whenAmountBelowMinimum_shouldErrorAndLog() {
        when(loanTypeRepository.existsByNameForAmount("HIPOTECARIO"))
                .thenReturn(Mono.just(loanType(BigInteger.valueOf(2_000_000), BigInteger.valueOf(10_000_000))));

        StepVerifier.create(validation.validateLoanType("HIPOTECARIO", BigInteger.valueOf(1_000_000)))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof BadRequestException);
                    assertTrue(err.getMessage().startsWith("El valor del prestamo 1000000"));
                })
                .verify();

        verify(loanTypeRepository).existsByNameForAmount("HIPOTECARIO");
        verify(logger).info(startsWith("El valor del prestamo"));
        verifyNoMoreInteractions(logger);
    }

    // ERROR: monto MAYOR al máximo
    @Test
    void validateLoanType_whenAmountAboveMaximum_shouldErrorAndLog() {
        when(loanTypeRepository.existsByNameForAmount("LIBRE_INVERSION"))
                .thenReturn(Mono.just(loanType(BigInteger.valueOf(500_000), BigInteger.valueOf(3_000_000))));

        StepVerifier.create(validation.validateLoanType("LIBRE_INVERSION", BigInteger.valueOf(4_000_000)))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof BadRequestException);
                    assertTrue(err.getMessage().startsWith("El valor del prestamo 4000000"));
                })
                .verify();

        verify(loanTypeRepository).existsByNameForAmount("LIBRE_INVERSION");
        verify(logger).info(startsWith("El valor del prestamo"));
        verifyNoMoreInteractions(logger);
    }
}
