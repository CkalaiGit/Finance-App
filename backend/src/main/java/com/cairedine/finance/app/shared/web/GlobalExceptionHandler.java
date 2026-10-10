package com.cairedine.finance.app.shared.web;

import com.cairedine.finance.app.shared.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Comparator;
import java.util.List;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 * Mappe les exceptions métier sur les codes HTTP appropriés.
 * Les erreurs internes (500) sont loggées avec stacktrace complète.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * TickerNotFoundException → 404 Not Found
     */
    @ExceptionHandler(TickerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleTickerNotFound(TickerNotFoundException ex) {
        log.debug("Ticker not found: {}", ex.getMessage());
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "TICKER_NOT_FOUND");
    }

    /**
     * MarketDataUnavailableException → 503 Service Unavailable
     */
    @ExceptionHandler(MarketDataUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleMarketDataUnavailable(MarketDataUnavailableException ex) {
        log.warn("Market data unavailable: {}", ex.getMessage());
        return problemDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "MARKET_DATA_UNAVAILABLE");
    }

    /**
     * TickerAlreadyInWatchlistException → 409 Conflict
     */
    @ExceptionHandler(TickerAlreadyInWatchlistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleTickerAlreadyInWatchlist(TickerAlreadyInWatchlistException ex) {
        log.debug("Ticker already in watchlist: {}", ex.getTicker());
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage(), "TICKER_ALREADY_IN_WATCHLIST");
    }

    /**
     * TickerNotInWatchlistException → 404 Not Found
     */
    @ExceptionHandler(TickerNotInWatchlistException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleTickerNotInWatchlist(TickerNotInWatchlistException ex) {
        log.debug("Ticker not in watchlist: {}", ex.getTicker());
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "TICKER_NOT_IN_WATCHLIST");
    }

    /**
     * InvalidTickerException → 400 Bad Request
     */
    @ExceptionHandler(InvalidTickerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidTicker(InvalidTickerException ex) {
        log.debug("Invalid ticker: {}", ex.getMessage());
        return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_TICKER");
    }

    /**
     * WatchlistException → 400 Bad Request (fallback for base class)
     */
    @ExceptionHandler(WatchlistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleWatchlistException(WatchlistException ex) {
        log.debug("Watchlist error: {}", ex.getMessage());
        return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "WATCHLIST_ERROR");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        ProblemDetail detail = validationProblemDetail(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(ex, detail, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * ConstraintViolationException → 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
                .toList();
        return validationProblemDetail(errors);
    }

    /**
     * AccessDeniedException → 403 Forbidden (not 500!)
     * Raised by @EnableMethodSecurity/@Secured when permission denied
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return problemDetail(HttpStatus.FORBIDDEN, "Access denied", "ACCESS_DENIED");
    }

    /**
     * AuthenticationException → 401 Unauthorized (not 500!)
     * Raised by Spring Security when authentication fails
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return problemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "UNAUTHENTICATED");
    }

    /**
     * Catch-all handler for unexpected exceptions → 500 Internal Server Error
     * Logs full stacktrace but never exposes internal details to client
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return problemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur interne s'est produite",
            "INTERNAL_SERVER_ERROR"
        );
    }

    private ProblemDetail validationProblemDetail(List<ValidationError> errors) {
        ProblemDetail detail = problemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR");
        detail.setProperty("errors", errors);
        return detail;
    }

    private ProblemDetail problemDetail(HttpStatus status, String message, String code) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }

    private record ValidationError(String field, String message) {}
}
