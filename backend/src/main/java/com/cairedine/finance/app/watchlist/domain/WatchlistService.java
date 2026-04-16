package com.cairedine.finance.app.watchlist.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service de domaine orchestrant la logique métier de la watchlist.
 * N'a pas de dépendance directe aux détails d'implémentation.
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final IWatchlistRepositoryPort watchlistRepository;

    /**
     * Ajoute un ticker à la watchlist de l'utilisateur
     */
    public void addTickerToWatchlist(String keycloakId, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Le ticker ne peut pas être vide");
        }

        // Vérifier que le ticker n'existe pas déjà
        if (watchlistRepository.existsTickerInWatchlist(keycloakId, ticker)) {
            throw new IllegalStateException(String.format("Le ticker %s existe déjà dans la watchlist", ticker));
        }

        watchlistRepository.addTickerToWatchlist(keycloakId, ticker);
    }

    /**
     * Supprime un ticker de la watchlist de l'utilisateur
     */
    public void removeTickerFromWatchlist(String keycloakId, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Le ticker ne peut pas être vide");
        }

        // Vérifier que le ticker existe
        if (!watchlistRepository.existsTickerInWatchlist(keycloakId, ticker)) {
            throw new IllegalStateException(String.format("Le ticker %s n'existe pas dans la watchlist", ticker));
        }

        watchlistRepository.removeTickerFromWatchlist(keycloakId, ticker);
    }

    /**
     * Récupère la watchlist complète de l'utilisateur
     */
    public Set<String> getUserWatchlist(String keycloakId) {
        return watchlistRepository.findAllTickersByKeycloakId(keycloakId);
    }

    /**
     * Vérifie si un ticker est dans la watchlist
     */
    public boolean isTickerInWatchlist(String keycloakId, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return watchlistRepository.existsTickerInWatchlist(keycloakId, ticker);
    }
}

