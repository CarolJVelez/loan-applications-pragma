package co.com.bancolombia.api.exceptions;

import co.com.bancolombia.model.exceptions.*;
import co.com.bancolombia.model.loanApplication.gateways.LoggerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import java.util.Map;


@RestControllerAdvice
public class GlobalErrorHandler {

    private final LoggerRepository logger;

    public GlobalErrorHandler(LoggerRepository logger) {
        this.logger = logger;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        String msg = ex.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Error de validación");
        logger.warn("Error de Bad Request = {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", HttpStatus.BAD_REQUEST.value(), "message", msg));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        logger.warn("No encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(LoanPendingException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(LoanPendingException ex) {
        logger.warn("El usuario tiene un prestamo en estado pendiente: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", HttpStatus.CONFLICT.value(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleAny(Throwable ex) {
        logger.error("Error interno : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "message", "Error interno del servidor"
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        logger.warn("Solicitud inválida: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        logger.warn("No autorizado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED.value(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenRoleException(ForbiddenRoleException ex) {
        logger.warn("No autorizado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "message", ex.getMessage()
                ));
    }
}