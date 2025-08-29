package co.com.bancolombia.model.loanType;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanType {

    private Long loanTypeId;
    private String code;
    private String name;
    private BigInteger minimumAmount;
    private BigInteger maximumAmount;
    private BigDecimal interestRate;
    private Boolean automaticValidation;
    private OffsetDateTime createdAt;
}
