package co.com.bancolombia.usecase.statusLoan;

import co.com.bancolombia.model.client.UserClientDetails;
import co.com.bancolombia.model.exceptions.NotFoundException;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.PageResult;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StatusUseCase {

    private final LoanApplicationRepository loanApplicationRepository;
    private final StatusRepository statusRepository;
    private final LoggerRepository logger;
    private final IUserClient userClient;

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
                                    .flatMap(items -> {
                                        // ------ NUEVO: construir lista de userIds y pedir al micro de autenticación ------
                                        var userIds = items.stream()
                                                .map(LoanApplication::getUserId)
                                                .filter(Objects::nonNull)
                                                .distinct()
                                                .toList();

                                        if (userIds.isEmpty()) {
                                            logger.info("Página sin userIds asociados (page={}, size={})", page, size);
                                            return Mono.just(new PageResult<>(items, page, size, total));
                                        }

                                        // 2) Llamada micro de autenticación
                                        return userClient.findByIds(userIds)
                                                .collectList()
                                                .map(users -> {
                                                    // Mapear id -> UserClientDetails
                                                    var userMap = users.stream()
                                                            .collect(Collectors.toMap(
                                                                    UserClientDetails::getUserId,
                                                                    u -> u,
                                                                    (a, b) -> a
                                                            ));

                                                    // 4) Mezclar: setear  en cada LoanApplication
                                                    items.forEach(loan -> {
                                                        var user = userMap.get(loan.getUserId());
                                                        if (user != null) {
                                                            loan.setBaseSalary(user.getBaseSalary());
                                                            loan.setNames((user.getName() + " " + user.getLastName()).trim());
                                                        }
                                                    });
                                                    logger.info("Usuarios obtenidos desde auth: {}", users.size());
                                                    var missingUsers = userIds.stream()
                                                            .filter(id -> !userMap.containsKey(id))
                                                            .toList();
                                                    if (!missingUsers.isEmpty()) {
                                                        logger.warn("Usuarios no encontrados en auth para IDs: {}", missingUsers);
                                                    }
                                                    return new PageResult<>(items, page, size, total);
                                                });
                                    })
                            );
                });
    }

}
