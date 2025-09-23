package co.com.bancolombia.api.mapper;

import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.request.UpdateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanApplicationMapper {

    @Mapping(target = "email", source = "email")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "loanTermMonths", source = "loanTermMonths")
    @Mapping(target = "loanType", source = "loanType")
    LoanApplication toModel(CreateLoanApplicationDTO dto);

    @Mapping(target = "loanApplicationId", source = "loanApplicationId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "document", source = "document")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "names", source = "names")
    @Mapping(target = "baseSalary", source = "baseSalary")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "loanTermMonths", source = "loanTermMonths")
    @Mapping(target = "loanType", source = "loanType")
    @Mapping(target = "interestRate", source = "interestRate")
    @Mapping(target = "maxIndebtedness", source = "maxIndebtedness")
    @Mapping(target = "currentLoanMonthlyPayment", source = "currentLoanMonthlyPayment")
    @Mapping(target = "totalApprovedLoansMonthlyPayment", source = "totalApprovedLoansMonthlyPayment")
    @Mapping(target = "availableIndebtedness", source = "availableIndebtedness")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "observations", source = "observations")
    LoanApplicationResponseDTO toDto(LoanApplication loanApplication);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "observations", source = "observations")
    LoanApplication toModel(UpdateLoanApplicationDTO dto);
}
