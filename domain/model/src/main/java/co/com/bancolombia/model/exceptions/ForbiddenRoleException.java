package co.com.bancolombia.model.exceptions;

public class ForbiddenRoleException extends RuntimeException {
    public ForbiddenRoleException(String message) {
        super(message);
    }
}
