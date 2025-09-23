package co.com.bancolombia.sqs.sender;

import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.notifications.CapacityRequest;
import co.com.bancolombia.model.notifications.MessageSQS;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import co.com.bancolombia.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Repository
@RequiredArgsConstructor
public class SqsLoanNotificationRepository implements LoanNotificationRepository {
    private final SQSSender sender;
    private final SQSSenderProperties props;
    private final ObjectMapper mapper;
    private final LoggerRepository  log;
    private final SnsAsyncClient sns;

    @Value("${publisher.sns.topicArn}")
    private String topicArn;

    @Override
    public Mono<String> sendCapacityValidationRequest(CapacityRequest req) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(req))
                .doOnNext(json -> log.info("[CAPACITY->SQS] queue={} payload={}", props.queueUrl(), json))
                .flatMap(json -> sender.sendTo(props.queueUrl(), json));
    }

@Override
public Mono<String> sendMessageUpdateLoan(MessageSQS message) {

    return Mono.fromCallable(() -> mapper.writeValueAsString(message))
            .flatMap(json -> {

                Mono<String> sendToSns = Mono.fromFuture(
                                sns.publish(PublishRequest.builder()
                                        .topicArn(topicArn)
                                        .message(json)
                                        .subject("Loan decision")
                                        .build()
                                )
                        ).map(PublishResponse::messageId)
                        .doOnSuccess(id -> log.info("[SNS EVENTS] msgId={}", id));

                Mono<String> sendToSqs = sender.sendTo(props.notificationsQueueUrl(), json)
                        .doOnSuccess(id -> log.info("[NOTIF->SQS] queue={} msgId={}", props.notificationsQueueUrl(), id));

                return Mono.when(sendToSns, sendToSqs).thenReturn("OK");
            });
}
}