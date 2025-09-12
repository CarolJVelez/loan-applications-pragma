package co.com.bancolombia.sqs.sender;

import co.com.bancolombia.model.notifications.MessageSQS;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class SqsLoanNotificationRepository implements LoanNotificationRepository {
    private final SQSSender sender;
    private final ObjectMapper mapper;

    @Override
    public Mono<String> sendMessageUpdateLoan(MessageSQS msg) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(msg))
                .flatMap(sender::send);
    }
}