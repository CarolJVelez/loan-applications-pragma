package co.com.bancolombia.r2dbc.reactiveLoanApplication;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public class MyReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanApplication/* change for domain model */,
        LoanApplicationEntity/* change for adapter model */,
        Long,
        MyReactiveRepository
> implements LoanApplicationRepository {
    public MyReactiveRepositoryAdapter(MyReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.mapBuilder(d, LoanApplication.LoanApplicationBuilder.class).build());
    }

    @Override
    public Mono<Boolean> existsByEmailAndStatus(String document, String status) {
        return repository.existsByEmailAndStatus(document, status);
    }

    @Override
    public Flux<LoanApplication> findByStatuses(Collection<String> statuses, int page, int size) {
        long limit = size;
        long offset = (long) page * size;
        String[] arr = statuses.toArray(new String[0]);
        return repository.findForStatuses(arr, limit, offset)
                .map(this::toEntity);
    }

    @Override
    public Mono<Long> countByStatuses(Collection<String> statuses) {
        String[] arr = statuses.toArray(new String[0]);
        return repository.countForStatuses(arr);
    }

    @Override
    public Mono<LoanApplication> findByEmailAndId(String email, Long id) {
        return repository.findByEmailAndLoanApplicationId(email,id)
                .map(this::toEntity);
    }

    @Override
    public Flux<LoanApplication> findAllByUserIdAndStatus(Long userId, String status) {
        return repository.findAllByUserIdAndStatus(userId,status)
                .map(this::toEntity);
    }
}
