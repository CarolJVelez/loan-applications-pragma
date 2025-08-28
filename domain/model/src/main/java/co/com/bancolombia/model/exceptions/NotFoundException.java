package co.com.bancolombia.model.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String m) {
        super(m);
    }
}