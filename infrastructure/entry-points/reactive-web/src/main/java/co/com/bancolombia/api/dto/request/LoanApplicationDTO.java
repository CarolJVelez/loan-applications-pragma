package co.com.bancolombia.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationDTO {

    private Long loanApplicationId;

    @NotBlank(message = "El correo electronico es Obligatorio")
    private String email;

    @NotNull(message = "El valor del prestamo es Obligatorio")
    private BigInteger amount;

    @NotNull(message = "El tiempo del prestamo es Obligatorio")
    @Min(1)
    @Max(120)
    private Integer loanTermMonths;

    @NotBlank(message = "El tipo de prestamo es Obligatorio")
    private String loanType;

}

