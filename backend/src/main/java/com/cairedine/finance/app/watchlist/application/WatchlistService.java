package com.cairedine.finance.app.watchlist.application;

import com.cairedine.finance.app.shared.exceptions.InvalidTickerException;
import com.cairedine.finance.app.shared.exceptions.TickerAlreadyInWatchlistException;
import com.cairedine.finance.app.shared.exceptions.TickerNotInWatchlistException;
import com.cairedine.finance.app.watchlist.TickerAddedEvent;
import com.cairedine.finance.app.watchlist.domain.IWatchlistRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Service de cas d'usage orchestrant la logique métier de la watchlist.
 * Déplacé de watchlist/domain/ vers watchlist/application/ pour respecter la pureté du domaine :
 * le domaine ne dépend d'aucun framework, @Service appartient à la couche application.
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final IWatchlistRepositoryPort watchlistRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Ajoute un ticker à la watchlist de l'utilisateur
     */
    @Transactional
    public void addTickerToWatchlist(String keycloakId, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException("Le ticker ne peut pas être vide");
        }

        // Vérifier que le ticker n'existe pas déjà
        if (watchlistRepository.existsTickerInWatchlist(keycloakId, ticker)) {
            throw new TickerAlreadyInWatchlistException(ticker);
        }

        watchlistRepository.addTickerToWatchlist(keycloakId, ticker);
        eventPublisher.publishEvent(new TickerAddedEvent(
                ticker.trim().toUpperCase(Locale.ROOT), Instant.now()));
    }

    /**
     * Supprime un ticker de la watchlist de l'utilisateur
     */
    public void removeTickerFromWatchlist(String keycloakId, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException("Le ticker ne peut pas être vide");
        }

        // Vérifier que le ticker existe
        if (!watchlistRepository.existsTickerInWatchlist(keycloakId, ticker)) {
            throw new TickerNotInWatchlistException(ticker);
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
