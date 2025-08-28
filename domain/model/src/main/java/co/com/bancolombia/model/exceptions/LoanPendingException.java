package co.com.bancolombia.model.exceptions;

public class LoanPendingException extends RuntimeException {
    public LoanPendingException(String document) {
        super("El usuario tiene un prestamo en esta Pendiente: " + document);
    }
}