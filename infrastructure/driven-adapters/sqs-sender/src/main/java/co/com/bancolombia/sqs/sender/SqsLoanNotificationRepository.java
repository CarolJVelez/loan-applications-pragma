package co.com.bancolombia.sqs.sender;

import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.notifications.CapacityRequest;
import co.com.bancolombia.model.notifications.MessageSQS;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import co.com.bancolombia.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class SqsLoanNotificationRepository implements LoanNotificationRepository {
    private final SQSSender sender;
    private final SQSSenderProperties props;
    private final ObjectMapper mapper;
    private final LoggerRepository  log;

    @Override
    public Mono<String> sendMessageUpdateLoan(MessageSQS msg) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(msg))
                .doOnNext(json -> log.info("[NOTIF->SQS] queue={} payload={}", props.notificationsQueueUrl(), json))
                .flatMap(json -> sender.sendTo(props.notificationsQueueUrl(), json));
    }

    @Override
    public Mono<String> sendCapacityValidationRequest(CapacityRequest req) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(req))
                .doOnNext(json -> log.info("[CAPACITY->SQS] queue={} payload={}", props.queueUrl(), json))
                .flatMap(json -> sender.sendTo(props.queueUrl(), json));
    }
}