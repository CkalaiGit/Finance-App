package com.cairedine.finance.app.shared.exceptions;

public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String symbol) {
        super("Market data unavailable for : " + symbol);
    }
}
