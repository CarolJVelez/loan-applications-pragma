package co.com.bancolombia.sqs.listener.dto;

import lombok.Data;

@Data
public class LoanUpdateMessage {
    private Long applicationId;
    private String email;
    private String newStatus;
    private String observations;
    private Double capacidadMaxima;
    private Double deudaMensualActual;
    private Double capacidadDisponible;
    private Double cuotaPrestamoNuevo;
}
