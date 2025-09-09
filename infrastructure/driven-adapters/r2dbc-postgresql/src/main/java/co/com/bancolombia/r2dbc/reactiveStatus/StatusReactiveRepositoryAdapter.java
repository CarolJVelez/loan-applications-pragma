package co.com.bancolombia.r2dbc.reactiveStatus;

import co.com.bancolombia.model.loanType.LoanType;
import co.com.bancolombia.model.status.LoanStatus;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.r2dbc.entities.LoanStatusEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.reactiveLoanType.MyReactiveRepositoryLoanType;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public class StatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanStatus,
        LoanStatusEntity,
        Long,
        StatusReactiveRepository> implements StatusRepository {

    public StatusReactiveRepositoryAdapter(StatusReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.mapBuilder(d, LoanStatus.LoanStatusBuilder.class).build());
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return repository.existsByName(name);
    }

   // @Override
  /*  public Flux<String> findExistingNamesIn(Collection<String> names) {
        return repository.findExistingNamesIn(names);
    }*/
}
