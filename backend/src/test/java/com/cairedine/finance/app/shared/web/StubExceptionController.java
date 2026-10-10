package com.cairedine.finance.app.shared.web;


import com.cairedine.finance.app.shared.exceptions.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StubExceptionController {

    @GetMapping("/test/ticker-not-found")
    void throwTickerNotFoundException() {
        throw new TickerNotFoundException("INVALID");
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
}
