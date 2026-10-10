package com.cairedine.finance.app.shared.exceptions;

import lombok.Getter;

/**
 * Exception levée lorsqu'on essaie de supprimer un ticker qui n'existe pas dans la watchlist.
 * HTTP 404 Not Found.
 */
@Getter
public class TickerNotInWatchlistException extends WatchlistException {

    private final String ticker;

    public TickerNotInWatchlistException(String ticker) {
        super(String.format("Le ticker %s n'existe pas dans la watchlist", ticker));
        this.ticker = ticker;
    }

}
