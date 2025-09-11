package co.com.bancolombia.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanApplicationResponseDTO {

    private Long loanApplicationId;
    private Long userId;
    private String document;
    private String email;
    private String names;
    private BigInteger amount;
    private BigInteger baseSalary;
    private BigDecimal interestRate;
    private Integer loanTermMonths;
    private Integer totalMonthly;
    private String loanType;
    private String status;
    private String observations;

}
