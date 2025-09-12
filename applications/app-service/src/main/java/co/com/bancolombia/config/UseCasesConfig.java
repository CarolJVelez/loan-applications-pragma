package co.com.bancolombia.config;


import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.model.loanType.gateways.LoanTypeRepository;
import co.com.bancolombia.model.notifications.gateways.LoanNotificationRepository;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.usecase.client.IUserClient;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import co.com.bancolombia.usecase.statusLoan.StatusUseCase;
import co.com.bancolombia.usecase.validation.LoanValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public LoanValidation loanValidation(
            LoanApplicationRepository loanApplicationRepository,
            LoanTypeRepository loanTypeRepository,
            LoggerRepository logger
    ) {
        return new LoanValidation(loanApplicationRepository, loanTypeRepository, logger);
    }

    @Bean
    public LoanApplicationCase loanApplicationCase(
            LoanApplicationRepository repo,
            LoanValidation loanValidation,
            LoggerRepository logger,
            IUserClient iUserClient,
            LoanNotificationRepository loanNotificationRepository
    ) {
        return new LoanApplicationCase(repo, loanValidation, logger, iUserClient, loanNotificationRepository);
    }

    @Bean
    public StatusUseCase statusUseCase(
           LoanApplicationRepository loanApplicationRepository,
           StatusRepository statusRepository,
           LoggerRepository logger,
           IUserClient userClient
    ) {
        return new StatusUseCase(loanApplicationRepository, statusRepository, logger,userClient);
    }
}
