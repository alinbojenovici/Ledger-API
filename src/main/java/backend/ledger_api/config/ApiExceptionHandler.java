package backend.ledger_api.config;

import backend.ledger_api.exception.AccountConflictException;
import backend.ledger_api.exception.AccountNotFoundException;
import backend.ledger_api.exception.AccountRequestException;
import backend.ledger_api.service.LocalizedMessageService;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final LocalizedMessageService messages;

    public ApiExceptionHandler(LocalizedMessageService messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AccountRequestException.class)
    public ResponseEntity<ApiError> invalid(AccountRequestException e, Locale locale) {
        return response(HttpStatus.BAD_REQUEST, e.getMessage(), e.getArguments(), locale);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> missing(AccountNotFoundException e, Locale locale) {
        return response(HttpStatus.NOT_FOUND, e.getMessage(), e.getArguments(), locale);
    }

    @ExceptionHandler(AccountConflictException.class)
    public ResponseEntity<ApiError> conflict(AccountConflictException e, Locale locale) {
        return response(HttpStatus.CONFLICT, e.getMessage(), e.getArguments(), locale);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String key, Object[] args, Locale locale) {
        return ResponseEntity.status(status).body(new ApiError(key, messages.getMessage(key, locale, args)));
    }

    public record ApiError(String code, String message) {
    }
}
