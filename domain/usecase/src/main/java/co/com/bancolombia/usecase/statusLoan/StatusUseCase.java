package co.com.bancolombia.usecase.statusLoan;

import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@RequiredArgsConstructor
public class StatusUseCase {

    private final LoanApplicationRepository loanApplicationRepository;
    private final StatusRepository statusRepository;
    private final LoggerRepository logger;

    public Mono<PageResult<LoanApplication>> list(int page, int size, Collection<String> states) {
        if (states == null || states.isEmpty()) {
            return Mono.error(new NotFoundException("Debes proporcionar al menos un estado"));
        }

        return Flux.fromIterable(states)
                .flatMap(name ->
                        statusRepository.existsByName(name)
                                .map(exists -> reactor.util.function.Tuples.of(name, exists))
                )
                .collectList()
                .flatMap(pairs -> {
                    var missing = pairs.stream()
                            .filter(t -> !t.getT2())
                            .map(reactor.util.function.Tuple2::getT1)
                            .toList();
                    if (!missing.isEmpty()) {
                        return Mono.error(new NotFoundException("Estado(s) no encontrados: " + String.join(", ", missing)));
                    }
                    return loanApplicationRepository.countByStatuses(states)
                            .flatMap(total -> loanApplicationRepository.findByStatuses(states, page, size)
                                    .collectList()
                                    .map(items -> new PageResult<>(items, page, size, total))
                            );
                });
    }

    @lombok.Value
    public static class PageResult<T> {
        java.util.List<T> content;
        int page;
        int size;
        long totalElements;
    }
}
