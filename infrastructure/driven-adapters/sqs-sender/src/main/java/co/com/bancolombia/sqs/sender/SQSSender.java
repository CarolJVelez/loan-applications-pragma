package co.com.bancolombia.sqs.sender;

import co.com.bancolombia.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender /*implements SomeGateway*/ {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    

    public Mono<String> sendTo(String queueUrl, String message) {
        return Mono.fromCallable(() -> {
                    log.info("[SQS SEND] queue={} size={}B body={}",
                            queueUrl, message != null ? message.getBytes().length : 0, message);
                    return SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(message)
                            .build();
                })
                .flatMap(req -> Mono.fromFuture(client.sendMessage(req)))
                .doOnNext(resp -> log.debug("[SQS SEND OK] queue={} messageId={}", queueUrl, resp.messageId()))
                .map(SendMessageResponse::messageId);
    }

}
