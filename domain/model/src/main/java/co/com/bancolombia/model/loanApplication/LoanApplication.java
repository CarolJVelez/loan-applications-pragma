package co.com.bancolombia.model.loanApplication;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplication {

    private Long loanApplicationId;
    private String document;
    private BigDecimal amount;
    private Integer loanTermMonths;
    private String loanType;
    private String status;
    private OffsetDateTime createdAt;
}
