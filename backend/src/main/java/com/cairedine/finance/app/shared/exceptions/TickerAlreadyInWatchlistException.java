package com.cairedine.finance.app.shared.exceptions;

import lombok.Getter;

/**
 * Exception levée lorsqu'on essaie d'ajouter un ticker qui existe déjà dans la watchlist.
 * HTTP 409 Conflict.
 */
@Getter
public class TickerAlreadyInWatchlistException extends WatchlistException {

    private final String ticker;

    public TickerAlreadyInWatchlistException(String ticker) {
        super(String.format("Le ticker %s existe déjà dans la watchlist", ticker));
        this.ticker = ticker;
    }

}
