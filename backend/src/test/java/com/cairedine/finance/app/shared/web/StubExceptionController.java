package com.cairedine.finance.app.shared.web;


import com.cairedine.finance.app.shared.exceptions.MarketDataUnavailableException;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.shared.exceptions.WatchlistException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StubExceptionController {

    @GetMapping("/test/watchlist-exception")
    void throwWatchlistException() {
        throw new WatchlistException("Watchlist already exists");
    }

    @GetMapping("/test/ticker-not-found")
    void throwTickerNotFoundException() {
        throw new TickerNotFoundException("INVALID");
    }

    @GetMapping("/test/market-data-unavailable")
    void throwMarketDataUnavailableException() {
        throw new MarketDataUnavailableException("AAPL");
    }
}
