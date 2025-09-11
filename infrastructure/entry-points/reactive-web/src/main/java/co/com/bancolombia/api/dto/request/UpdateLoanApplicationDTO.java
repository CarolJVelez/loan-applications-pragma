package co.com.bancolombia.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoanApplicationDTO {

    //@NotBlank(message = "El id del prestamo es obligatorio")
    private Long loanApplicationId;

    @Email(message = "El correo electronico es inválido")
    @NotBlank(message = "El correo electronico es obligatorio")
    private String email;

    @NotBlank(message = "El estado es Obligatorio")
    private String status;

    @NotBlank(message = "Las observaciones del credito son Obligatorias")
    private String observations;

}
