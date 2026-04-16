package com.cairedine.finance.app.watchlist.infrastructure.persistence.repository;

import com.cairedine.finance.app.watchlist.infrastructure.persistence.entity.WatchlistItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la gestion de la watchlist en base de données.
 */
public interface IWatchlistJpaRepository extends JpaRepository<WatchlistItemJpaEntity, Long> {

    List<WatchlistItemJpaEntity> findByKeycloakId(String keycloakId);

    Optional<WatchlistItemJpaEntity> findByKeycloakIdAndTicker(String keycloakId, String ticker);

    boolean existsByKeycloakIdAndTicker(String keycloakId, String ticker);

    @Modifying
    void deleteByKeycloakIdAndTicker(String keycloakId, String ticker);

    @Modifying
    void deleteByKeycloakId(String keycloakId);
}

