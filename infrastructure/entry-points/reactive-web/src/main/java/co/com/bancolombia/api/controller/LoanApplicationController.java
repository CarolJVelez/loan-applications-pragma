package co.com.bancolombia.api.controller;

import co.com.bancolombia.api.HandlerLoanApplication;
import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/solicitud")
@RequiredArgsConstructor
@OpenAPIDefinition(info = @Info(
        title = "Documentación de solicitudes de prestamos",
        version = "v1",
        description = "Endpoints para gestión de prestamos"))
public class LoanApplicationController {

    private final HandlerLoanApplication handlerLoanApplication;
    private final LoanApplicationMapper mapper;

    @Operation(summary = "Crear prestamo", tags = {"Prestamo"})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public Mono<ResponseEntity<LoanApplicationResponseDTO>> create(@Valid @RequestBody CreateLoanApplicationDTO body) {
        return handlerLoanApplication.createLoan(body)
                .map(saved -> ResponseEntity
                        .created(URI.create("/api/v1/solicitud/" + saved.getLoanApplicationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.toDto(saved)));
    }

}