package com.cairedine.finance.app.watchlist.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Agrégat de domaine représentant la watchlist d'un utilisateur.
 * Encapsule la logique métier de gestion de la watchlist.
 */
public record WatchlistAggregate(String keycloakId, Set<String> tickers) {

    public WatchlistAggregate {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("keycloakId ne peut pas être null ou vide");
        }
        // Créer une copie immutable
        tickers = tickers == null ? new HashSet<>() : new HashSet<>(tickers);
    }

    /**
     * Crée une watchlist vide pour un utilisateur
     */
    public static WatchlistAggregate empty(String keycloakId) {
        return new WatchlistAggregate(keycloakId, new HashSet<>());
    }

    /**
     * Ajoute un ticker à la watchlist
     */
    public WatchlistAggregate addTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker ne peut pas être null ou vide");
        }
        Set<String> newTickers = new HashSet<>(this.tickers);
        newTickers.add(ticker.toUpperCase());
        return new WatchlistAggregate(this.keycloakId, newTickers);
    }

    /**
     * Supprime un ticker de la watchlist
     */
    public WatchlistAggregate removeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker ne peut pas être null ou vide");
        }
        Set<String> newTickers = new HashSet<>(this.tickers);
        newTickers.remove(ticker.toUpperCase());
        return new WatchlistAggregate(this.keycloakId, newTickers);
    }

    /**
     * Vérifie si un ticker est dans la watchlist
     */
    public boolean containsTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return this.tickers.contains(ticker.toUpperCase());
    }

    /**
     * Retourne une copie immutable des tickers
     */
    @Override
    public Set<String> tickers() {
        return Set.copyOf(tickers);
    }
}

