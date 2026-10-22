package com.cairedine.finance.app.shared.exceptions;

/**
 * Exception levée lorsqu'un ticker est invalide (null, vide ou mal formé).
 * HTTP 400 Bad Request.
 */
public class InvalidTickerException extends WatchlistException {

    public InvalidTickerException(String message) {
        super(message);
    }

    public InvalidTickerException(String message, Throwable cause) {
        super(message, cause);
    }
}
