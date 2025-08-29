package co.com.bancolombia.r2dbc.reactiveLoanType;

import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.loanType.gateways.LoanTypeRepository;
import co.com.bancolombia.r2dbc.entities.LoanTypeEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class MyReactiveRepositoryAdapterLoanType extends ReactiveAdapterOperations<
        LoanType/* change for domain model */,
        LoanTypeEntity/* change for adapter model */,
        Long,
        MyReactiveRepositoryLoanType
> implements LoanTypeRepository {
    public MyReactiveRepositoryAdapterLoanType(MyReactiveRepositoryLoanType repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.mapBuilder(d, LoanType.LoanTypeBuilder.class).build());
    }


    @Override
    public Mono<Boolean> existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public Mono<LoanType> existsByNameForAmount(String name) {
        return repository.findByName(name);
    }
}
