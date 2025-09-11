package co.com.bancolombia.usecase.validation;


import co.com.bancolombia.model.exceptions.BadRequestException;
import co.com.bancolombia.model.exceptions.LoanPendingException;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.loanType.gateways.LoanTypeRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public final class LoanValidation {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoggerRepository logger;

    public LoanValidation(LoanApplicationRepository loanApplicationRepository,
                                     LoanTypeRepository loanTypeRepository,
                                     LoggerRepository logger) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanTypeRepository = loanTypeRepository;
        this.logger = logger;
    }

    public Mono<Void> validateNoPendingLoan(String email, String status) {
        return loanApplicationRepository.existsByEmailAndStatus(email, status)
                .flatMap(hasPending -> {
                    if (hasPending) {
                        logger.info("Tienes un prestamo en estado pendiente");
                        return Mono.error(new LoanPendingException(email));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Void> validateLoanTypeExists(String loanTypeName) {
        return loanTypeRepository.existsByName(loanTypeName)
                .flatMap(exist -> {
                    if (!exist) {
                        String error = "Tipo de prestamo no valido: " + loanTypeName;
                        logger.info(error);
                        return Mono.error(new NotFoundException(error));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Void> validateLoanType(String loanTypeName, BigInteger amount) {
        return loanTypeRepository.existsByNameForAmount(loanTypeName)
                .flatMap(loanType -> {
                    if (amount.compareTo(loanType.getMinimumAmount()) < 0
                            || amount.compareTo(loanType.getMaximumAmount()) > 0 ) {
                        String error = "El valor del prestamo " + amount + " se sale del rango permitodo: ";
                        logger.info(error);
                        return Mono.error(new BadRequestException(error));
                    }
                    return Mono.empty();
                });
    }

    public Mono<LoanType> validateAndGetLoanType(String loanTypeName, BigInteger amount) {
        return loanTypeRepository.existsByNameForAmount(loanTypeName)
                .switchIfEmpty(Mono.error(new NotFoundException("No existe el tipo de préstamo: " + loanTypeName)))
                .flatMap(loanType -> {
                    if (amount.compareTo(loanType.getMinimumAmount()) < 0
                            || amount.compareTo(loanType.getMaximumAmount()) > 0) {
                        String error = String.format(
                                "El valor del préstamo %s se sale del rango permitido [%s - %s].",
                                amount, loanType.getMinimumAmount(), loanType.getMaximumAmount());
                        logger.info(error);
                        return Mono.error(new BadRequestException(error));
                    }
                    return Mono.just(loanType);
                });
    }

    public Integer calculateTotalMonthly(BigInteger amount, BigDecimal interestRate, Integer loanTermMonths) {
        MathContext mc = new MathContext(15, RoundingMode.HALF_UP);
        BigDecimal p = new BigDecimal(amount);
        BigDecimal tasaMensual = interestRate
                .divide(BigDecimal.valueOf(100), mc)
                .divide(BigDecimal.valueOf(12), mc);

        if (tasaMensual.compareTo(BigDecimal.ZERO) == 0) {
            // Si la tasa es 0%, cuota = capital / meses
            return p.divide(BigDecimal.valueOf(loanTermMonths), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        BigDecimal unoMasI = BigDecimal.ONE.add(tasaMensual, mc);
        BigDecimal pow = unoMasI.pow(loanTermMonths, mc);

        // cuota = P * i * (1+i)^n / ((1+i)^n - 1)
        BigDecimal num = p.multiply(tasaMensual, mc).multiply(pow, mc);
        BigDecimal den = pow.subtract(BigDecimal.ONE, mc);

        BigDecimal totalMonthly = num.divide(den, 2, RoundingMode.HALF_UP);

        // Redondeamos al entero más cercano
        return totalMonthly.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public Mono<LoanApplication> validateExistLoan(String email, Long loanApplicationId) {
        return loanApplicationRepository.findByEmailAndId(email, loanApplicationId)
                .doOnNext(loan -> logger.info("Préstamo encontrado: {}", loan))
                .switchIfEmpty(Mono.error(new NotFoundException("No existe el préstamo del usuario con correo: " + email)));
    }
}

