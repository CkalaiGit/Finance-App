package com.cairedine.finance.app.shared.exceptions;


public class TickerNotFoundException extends RuntimeException {

    public TickerNotFoundException(String symbol) {
        super("Ticker not found : " + symbol);
    }
}