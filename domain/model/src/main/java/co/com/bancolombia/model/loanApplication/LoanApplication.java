package co.com.bancolombia.model.loanApplication;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplication {

    private Long loanApplicationId;
    private Long userId;
    private String document;
    private String email;
    private String names;
    private BigInteger amount;
    private Integer loanTermMonths;
    private String loanType;
    private String status;
    private BigInteger baseSalary;
    private Integer totalMonthly;
    private BigDecimal interestRate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String observations;
}
