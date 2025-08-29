package co.com.bancolombia.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponseDTO {

    private Long loanApplicationId;
    private Long userId;
    private String document;
    private String email;
    private BigInteger amount;
    private Integer loanTermMonths;
    private String loanType;
    private String status;
    private OffsetDateTime createdAt;

}
