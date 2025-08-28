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
import reactor.core.publisher.Flux;
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
    public Mono<ResponseEntity<LoanApplicationResponseDTO>> create(@Valid @RequestBody CreateLoanApplicationDTO body) {
        return handlerLoanApplication.createUser(body)
                .map(saved -> ResponseEntity
                        .created(URI.create("/api/v1/solicitud/" + saved.getLoanApplicationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.toDto(saved)));
    }
/*
    @Operation(summary = "Listar Usuarios", tags = {"ListUsuarios"})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<LoanApplicationResponseDTO> listAllUser(){
        return handlerLoanApplication.listAllUsers()
                .map(mapper::toDto);
    }

    @Operation(summary = "Obtener usuario por id", tags = {"Usuarios"})
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoanApplicationResponseDTO>> findUserById(@PathVariable("id")  Long id) {
        return handlerLoanApplication.findUserById(id)
                .map(user -> ResponseEntity.ok(mapper.toDto(user)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }*/
}