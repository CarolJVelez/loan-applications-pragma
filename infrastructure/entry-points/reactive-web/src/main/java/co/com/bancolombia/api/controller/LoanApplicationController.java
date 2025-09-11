package co.com.bancolombia.api.controller;

import co.com.bancolombia.api.HandlerLoanApplication;
import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.request.UpdateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.api.dto.response.PageDTO;
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
import java.util.List;

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

    @Operation(summary = "Listado simple por estado", tags = {"Prestamo"})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ASESOR')")
    public Mono<ResponseEntity<PageDTO<LoanApplicationResponseDTO>>> listSimple(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "estado") List<String> estados
    ) {
        return handlerLoanApplication.listSimple(page, size, estados)
                .map(pr -> {
                    var content = pr.getContent().stream().map(mapper::toDto).toList();
                    long totalPages = (long) Math.ceil(pr.getTotalElements() / (double) pr.getSize());
                    return ResponseEntity.ok(
                            PageDTO.<LoanApplicationResponseDTO>builder()
                                    .content(content)
                                    .page(pr.getPage())
                                    .size(pr.getSize())
                                    .totalElements(pr.getTotalElements())
                                    .totalPages(totalPages)
                                    .build()
                    );
                });
    }

    @Operation(summary = "Actualizar prestamo", tags = {"Prestamo"})
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ASESOR')")
    public Mono<ResponseEntity<LoanApplicationResponseDTO>> update(@Valid @RequestBody UpdateLoanApplicationDTO body) {
        return handlerLoanApplication.updateLoan(body)
                .map(saved -> ResponseEntity
                        .created(URI.create("/api/v1/solicitud/" + saved.getLoanApplicationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.toDto(saved)));
    }

}