package co.com.bancolombia.model.notifications.gateways;

import co.com.bancolombia.model.notifications.DecisionEvent;
import reactor.core.publisher.Mono;

public interface DecisionPublisher {
    Mono<Void> publish(DecisionEvent event);

}
