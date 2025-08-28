package co.com.bancolombia.api.mapper;

import co.com.bancolombia.api.dto.request.CreateLoanApplicationDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.model.loanApplication.LoanApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanApplicationMapper {

    @Mapping(target = "document", source = "documentoIdentidad")
    @Mapping(target = "amount", source = "valorPrestamo")
    @Mapping(target = "loanTermMonths", source = "tiempoPrestamoMeses")
    @Mapping(target = "loanType", source = "tipoPrestamo")
    LoanApplication toModel(CreateLoanApplicationDTO dto);

    @Mapping(target = "prestamoId", source = "loanApplicationId")
    @Mapping(target = "documentoIdentidad", source = "document")
    @Mapping(target = "valorPrestamo", source = "amount")
    @Mapping(target = "tiempoPrestamoMeses", source = "loanTermMonths")
    @Mapping(target = "tipoPrestamo", source = "loanType")
    @Mapping(target = "estado", source = "status")
    @Mapping(target = "fechaCreacion", source = "createdAt")
    LoanApplicationResponseDTO toDto(LoanApplication loanApplication);
}
