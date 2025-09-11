package co.com.bancolombia.sqs;


import co.com.bancolombia.model.notifications.DecisionEvent;
import co.com.bancolombia.model.notifications.gateways.DecisionPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@RequiredArgsConstructor
public class SqsDecisionPublisher implements DecisionPublisher {

    private final SqsAsyncClient sqs;
    private final ObjectMapper mapper;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    @Override
    public Mono<Void> publish(DecisionEvent e) {
        return Mono.fromFuture(() -> {
            try {
                String body = mapper.writeValueAsString(e);
                return sqs.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        // Si usas FIFO, descomenta:
                        //.messageGroupId(String.valueOf(e.getSolicitudId()))
                        //.messageDeduplicationId(e.getSolicitudId() + "-" + e.getNuevoEstado())
                        .build());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).then();
    }
}