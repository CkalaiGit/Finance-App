package com.cairedine.finance.app.shared.web;

import com.cairedine.finance.app.shared.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * MarketDataUnavailableException → 503 Service Unavailable
     */
    @ExceptionHandler(MarketDataUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleMarketDataUnavailable(MarketDataUnavailableException ex) {
        log.warn("Market data unavailable: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * TickerAlreadyInWatchlistException → 409 Conflict
     */
    @ExceptionHandler(TickerAlreadyInWatchlistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleTickerAlreadyInWatchlist(TickerAlreadyInWatchlistException ex) {
        log.debug("Ticker already in watchlist: {}", ex.getTicker());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setProperty("code", "TICKER_ALREADY_IN_WATCHLIST");
        return detail;
    }

    /**
     * TickerNotInWatchlistException → 404 Not Found
     */
    @ExceptionHandler(TickerNotInWatchlistException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleTickerNotInWatchlist(TickerNotInWatchlistException ex) {
        log.debug("Ticker not in watchlist: {}", ex.getTicker());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setProperty("code", "TICKER_NOT_IN_WATCHLIST");
        return detail;
    }

    /**
     * InvalidTickerException → 400 Bad Request
     */
    @ExceptionHandler(InvalidTickerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidTicker(InvalidTickerException ex) {
        log.debug("Invalid ticker: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setProperty("code", "INVALID_TICKER");
        return detail;
    }

    /**
     * WatchlistException → 400 Bad Request (fallback for base class)
     */
    @ExceptionHandler(WatchlistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleWatchlistException(WatchlistException ex) {
        log.debug("Watchlist error: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * AccessDeniedException → 403 Forbidden (not 500!)
     * Raised by @EnableMethodSecurity/@Secured when permission denied
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    /**
     * AuthenticationException → 401 Unauthorized (not 500!)
     * Raised by Spring Security when authentication fails
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    /**
     * Catch-all handler for unexpected exceptions → 500 Internal Server Error
     * Logs full stacktrace but never exposes internal details to client
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur interne s'est produite"
        );
        detail.setProperty("code", "INTERNAL_SERVER_ERROR");
        return detail;
    }
}

