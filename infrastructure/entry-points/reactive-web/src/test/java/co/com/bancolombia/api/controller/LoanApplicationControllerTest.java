package co.com.bancolombia.api.controller;
import co.com.bancolombia.api.HandlerLoanApplication;
import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.api.exceptions.GlobalErrorHandler;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import co.com.bancolombia.model.loanApplication.LoanTyoe;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;

@WebFluxTest(controllers = LoanApplicationController.class)
@Import(GlobalErrorHandler.class) // para que el advice formatee errores
@ContextConfiguration(classes = {LoanApplicationController.class}) // registra el controller explícito
class LoanApplicationControllerTest {

    @Autowired
    WebTestClient client;

    @MockitoBean
    HandlerLoanApplication handlerLoanApplication;
    @MockitoBean
    LoanApplicationMapper mapper;
    @MockitoBean
    LoggerRepository logger;

    // ---------- TEST 1: Éxito (201) ----------
    @Test
    void whenValidRequest_then201() {
        var dto = new CreateLoanApplicationDTO(
                "Carol", "Velez", LocalDate.parse("1996-04-10"),
                "Cra 1 # 23-45", "3172985404",
                "carol@example.com", BigInteger.valueOf(2_500_000)
        );

        // Dominio que retorna el handler
        var user = LoanTyoe.builder()
                .userId(123L)
                .name("Carol")
                .lastname("Velez")
                .birthDate(LocalDate.parse("1996-04-10"))
                .address("Cra 1 # 23-45")
                .phone("3172985404")
                .email("carol@example.com")
                .baseSalary(BigInteger.valueOf(2_500_000))
                .build();

        var response = new LoanApplicationResponseDTO(
                123L, "Carol", "Velez", LocalDate.parse("1996-04-10"),
                "Cra 1 # 23-45", "3172985404", "carol@example.com",
                BigInteger.valueOf(2_500_000)
        );

        Mockito.when(handlerLoanApplication.createUser(any(CreateLoanApplicationDTO.class))).thenReturn(Mono.just(user));
        Mockito.when(mapper.toDto(any(LoanTyoe.class))).thenReturn(response);

        client.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/usuarios/123")
                .expectBody()
                .jsonPath("$.userId").isEqualTo("123")
                .jsonPath("$.nombres").isEqualTo("Carol");
    }

    // ---------- TEST 2: Validación (400) ----------
    @Test
    void whenInvalidTelefono_then400() {
        var dto = new CreateLoanApplicationDTO(
                "Carol", "Velez", LocalDate.parse("1996-04-10"),
                "Cra 1 # 23-45", "",
                "carol@example.com", BigInteger.valueOf(2_500_000)
        );

        client.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("El telefono es obligatorio");
    }
}