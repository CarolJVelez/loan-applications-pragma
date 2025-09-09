package co.com.bancolombia.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestValidator {
    private final Validator validator;

    public <T> Mono<T> validate(T payload) {
        Set<ConstraintViolation<T>> v = validator.validate(payload);
        if (!v.isEmpty()) {
            String msg = v.stream().map(x -> x.getPropertyPath() + ": " + x.getMessage()).collect(Collectors.joining("; "));
            return Mono.error(new IllegalArgumentException(msg));
        }
        return Mono.just(payload);
    }

}
