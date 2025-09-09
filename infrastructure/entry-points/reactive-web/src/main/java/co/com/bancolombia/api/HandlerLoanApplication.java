package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import co.com.bancolombia.model.exceptions.UnauthorizedException;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.PageResult;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import co.com.bancolombia.usecase.statusLoan.StatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HandlerLoanApplication {

    private final LoanApplicationCase loanApplicationCase;
    private final LoanApplicationMapper mapper;
    private final LoggerRepository logger;
    private final StatusUseCase statusUseCase;

    public Mono<LoanApplication> createLoan(CreateLoanApplicationDTO body) {
        logger.info("POST /api/v1/solicitud");
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String emailToken = auth.getToken().getSubject();
                    if (!emailToken.equalsIgnoreCase(body.getEmail())) {
                        logger.warn("Intento de suplantación: token={} body={}", emailToken, body.getEmail());
                        return Mono.error(new UnauthorizedException("Solo puedes crear solicitudes de préstamo para ti mismo"));
                    }
                    return loanApplicationCase.create(mapper.toModel(body));
                });
    }

    public Mono<PageResult<LoanApplication>>
    listSimple(int page, int size, List<String> estados) {
        logger.info("GET /api/v1/solicitud (simple) page={} size={} estados={}", page, size, estados);
        return statusUseCase.list(page, size, estados);
    }

}