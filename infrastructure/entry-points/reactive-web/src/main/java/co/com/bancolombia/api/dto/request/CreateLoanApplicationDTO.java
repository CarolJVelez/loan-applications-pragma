package co.com.bancolombia.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanApplicationDTO {

    @NotBlank(message = "El documento de identidad es Obligatorio")
    private String documentoIdentidad;

    @NotNull(message = "El valor del prestamo es Obligatorio")
    private BigInteger valorPrestamo;

    @NotNull(message = "El tiempo del prestamo es Obligatorio")
    @Min(1)
    @Max(120)
    private Integer tiempoPrestamoMeses;

    @NotBlank(message = "El tipo de prestamo es Obligatorio")
    private String tipoPrestamo;

}
