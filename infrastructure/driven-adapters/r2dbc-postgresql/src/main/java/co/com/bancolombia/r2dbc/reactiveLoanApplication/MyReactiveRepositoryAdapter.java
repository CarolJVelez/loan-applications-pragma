package co.com.bancolombia.r2dbc.reactiveLoanApplication;

import co.com.bancolombia.model.loanApplication.LoanApplication;
import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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

}
