package com.cairedine.finance.app.shared.web;


import com.cairedine.finance.app.shared.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
class StubExceptionController {

    @GetMapping("/test/ticker-not-found")
    void throwTickerNotFoundException() {
        throw new TickerNotFoundException("INVALID");
    }

    @GetMapping("/test/watchlist-error")
    void throwWatchlistException() {
        throw new StubWatchlistException("Watchlist error");
    }

    @GetMapping("/test/market-data-unavailable")
    void throwMarketDataUnavailableException() {
        throw new MarketDataUnavailableException("AAPL");
    }

    @GetMapping("/test/ticker-already-in-watchlist")
    void throwTickerAlreadyInWatchlistException() {
        throw new TickerAlreadyInWatchlistException("AAPL");
    }

    @GetMapping("/test/ticker-not-in-watchlist")
    void throwTickerNotInWatchlistException() {
        throw new TickerNotInWatchlistException("MSFT");
    }

    @GetMapping("/test/invalid-ticker")
    void throwInvalidTickerException() {
        throw new InvalidTickerException("Le ticker ne peut pas être vide");
    }

    @GetMapping("/test/unexpected-exception")
    void throwUnexpectedException() {
        throw new RuntimeException("This is an internal error that should not be exposed to client");
    }

    @GetMapping("/test/illegal-state-exception")
    void throwIllegalStateException() {
        throw new IllegalStateException("This is an internal JDK error that should not be mapped to 400");
    }

    @GetMapping("/test/access-denied")
    void throwAccessDeniedException() {
        throw new AccessDeniedException("Not allowed");
    }

    @GetMapping("/test/unauthenticated")
    void throwAuthenticationException() {
        throw new AuthenticationServiceException("Invalid authentication");
    }

    @PostMapping("/test/invalid-body")
    void validateBody(@Valid @RequestBody InvalidBodyRequest request) {}

    @GetMapping("/test/constraint-violation")
    void throwConstraintViolation() {
        throw new ConstraintViolationException("Invalid request", Set.of());
    }

    private record InvalidBodyRequest(@NotBlank String name) {}

    private static class StubWatchlistException extends WatchlistException {
        StubWatchlistException(String message) {
            super(message);
        }
    }
}
