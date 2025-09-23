package co.com.bancolombia.sqs.listener;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.sqs.listener.dto.LoanUpdateMessage;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {


    private final LoanApplicationCase loanApplicationCase;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LoggerRepository logger;

    @Override
    public Mono<Void> apply(Message message) {
        logger.info("[SQS RECV] messageId={} body={}", message.messageId(), message.body());

        return Mono.fromCallable(() -> mapper.readValue(message.body(), LoanUpdateMessage.class))
                .flatMap(payload -> {
                    logger.info("[SQS PARSED] appId={} newStatus={} email={} obs={}",
                            payload.getApplicationId(), payload.getNewStatus(), payload.getEmail(), payload.getObservations());

                    LoanApplication updateReq = LoanApplication.builder()
                            .loanApplicationId(payload.getApplicationId())
                            .email(payload.getEmail())
                            .status(payload.getNewStatus())
                            .observations(payload.getObservations())
                            .updatedAt(OffsetDateTime.now(ZoneId.of("America/Bogota")))
                            .build();

                    return loanApplicationCase.update(updateReq)
                            .doOnSuccess(saved -> logger.info("[UPDATE OK] id={} status={} email={}",
                                    saved.getLoanApplicationId(), saved.getStatus(), saved.getEmail()))
                            .then();
                })
                .doOnError(err -> logger.error("[SQS PROCESS ERROR] messageId={} error={}", message.messageId(), err.getMessage(), err));
    }
}
