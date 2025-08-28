package co.com.bancolombia.api.dto.response;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponseDTO {

    private Long prestamoId;
    private String documentoIdentidad;
    private BigDecimal valorPrestamo;
    private Integer tiempoPrestamoMeses;
    private String tipoPrestamo;
    private String estado;
    private OffsetDateTime fechaCreacion;

}
