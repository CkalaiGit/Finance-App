package com.cairedine.finance.app.watchlist.domain;

import java.util.Optional;
import java.util.Set;

/**
 * Port d'interface pour la persistance de la watchlist.
 * Définit les opérations métier de gestion de la watchlist.
 */
public interface IWatchlistRepositoryPort {

    Optional<WatchlistAggregate> findByKeycloakId(String keycloakId);

    void save(String keycloakId, WatchlistAggregate watchlist);

    boolean existsTickerInWatchlist(String keycloakId, String ticker);

    void addTickerToWatchlist(String keycloakId, String ticker);

    void removeTickerFromWatchlist(String keycloakId, String ticker);

    Set<String> findAllTickersByKeycloakId(String keycloakId);
}

