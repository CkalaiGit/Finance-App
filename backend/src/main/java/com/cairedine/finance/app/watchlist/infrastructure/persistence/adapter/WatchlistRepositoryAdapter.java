package com.cairedine.finance.app.watchlist.infrastructure.persistence.adapter;

import com.cairedine.finance.app.watchlist.domain.IWatchlistRepositoryPort;
import com.cairedine.finance.app.watchlist.domain.WatchlistAggregate;
import com.cairedine.finance.app.watchlist.infrastructure.persistence.entity.WatchlistItemJpaEntity;
import com.cairedine.finance.app.watchlist.infrastructure.persistence.repository.IWatchlistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptateur d'infrastructure implémentant le port de repository.
 * Encapsule la logique d'accès aux données JPA.
 * Aucune logique métier ici, seulement la traduction entre
 * l'agrégat de domaine et l'entité JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class WatchlistRepositoryAdapter implements IWatchlistRepositoryPort {

    private final IWatchlistJpaRepository iWatchlistJpaRepository;

    @Override
    public Optional<WatchlistAggregate> findByKeycloakId(String keycloakId) {
        Set<String> tickers = iWatchlistJpaRepository.findAllTickersByKeycloakId(keycloakId);
        return Optional.of(new WatchlistAggregate(keycloakId, tickers));
    }

    @Override
    public void save(String keycloakId, WatchlistAggregate watchlist) {
        // Nettoyer les anciens items
        iWatchlistJpaRepository.deleteAllByKeycloakId(keycloakId);

        // Créer les nouveaux items
        var items = watchlist.tickers().stream()
            .map(ticker -> WatchlistItemJpaEntity.builder()
                .keycloakId(keycloakId)
                .ticker(ticker)
                .build())
            .collect(Collectors.toList());

        iWatchlistJpaRepository.saveAll(items);
    }

    @Override
    public boolean existsTickerInWatchlist(String keycloakId, String ticker) {
        return iWatchlistJpaRepository.existsByKeycloakIdAndTicker(keycloakId, ticker.toUpperCase());
    }

    @Override
    public void addTickerToWatchlist(String keycloakId, String ticker) {
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(keycloakId)
            .ticker(ticker.toUpperCase())
            .build();

        iWatchlistJpaRepository.save(item);
    }

    @Override
    public void removeTickerFromWatchlist(String keycloakId, String ticker) {
        iWatchlistJpaRepository.deleteByKeycloakIdAndTicker(keycloakId, ticker.toUpperCase());
    }

    @Override
    public Set<String> findAllTickersByKeycloakId(String keycloakId) {
        return iWatchlistJpaRepository.findAllTickersByKeycloakId(keycloakId);
    }
}

