package co.com.bancolombia.config;

import co.com.bancolombia.model.loanApplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import co.com.bancolombia.usecase.loanApplication.LoanApplicationCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
public class UseCasesConfig {

    @Bean
    public LoanApplicationCase loanApplicationCase(LoanApplicationRepository repo, LoggerRepository logger) {
        return new LoanApplicationCase(repo, logger);
    }
}