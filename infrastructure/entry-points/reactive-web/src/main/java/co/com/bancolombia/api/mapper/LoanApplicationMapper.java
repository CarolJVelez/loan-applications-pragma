package co.com.bancolombia.api.mapper;

import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
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
    @Mapping(target = "name", source = "name")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "loanTermMonths", source = "loanTermMonths")
    @Mapping(target = "loanType", source = "loanType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    LoanApplicationResponseDTO toDto(LoanApplication loanApplication);
}
