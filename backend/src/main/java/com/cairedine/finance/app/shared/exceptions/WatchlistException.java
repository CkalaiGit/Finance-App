package com.cairedine.finance.app.shared.exceptions;

/**
 * Exception de base pour les erreurs métier liées à la watchlist.
 * Abstraite : utiliser les sous-classes spécialisées pour chaque cas d'erreur.
 */
public abstract class WatchlistException extends RuntimeException {

    public WatchlistException(String message) {
        super(message);
    }

    public WatchlistException(String message, Throwable cause) {
        super(message, cause);
    }
}