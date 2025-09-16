package co.com.bancolombia.model.notifications.gateways;

import co.com.bancolombia.model.notifications.CapacityRequest;
import co.com.bancolombia.model.notifications.MessageSQS;
import reactor.core.publisher.Mono;

public interface LoanNotificationRepository {

    Mono<String> sendMessageUpdateLoan(MessageSQS message);

    Mono<String> sendCapacityValidationRequest(CapacityRequest request);

}
