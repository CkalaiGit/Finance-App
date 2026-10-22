package com.cairedine.finance.app.watchlist.infrastructure.persistence.repository;

import com.cairedine.finance.app.watchlist.infrastructure.persistence.entity.WatchlistItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Repository JPA optimisé pour la gestion de la watchlist.
 * Utilise des requêtes JPQL explicites pour la performance et l'alignement avec les besoins du domaine.
 */
public interface IWatchlistJpaRepository extends JpaRepository<WatchlistItemJpaEntity, Long> {

    /**
     * Récupère uniquement les tickers suivis par un utilisateur.
     * Optimisé pour l'architecture hexagonale (évite le chargement d'entités complètes).
     */
    @Query("SELECT w.ticker FROM WatchlistItemJpaEntity w WHERE w.keycloakId = :keycloakId")
    Set<String> findAllTickersByKeycloakId(@Param("keycloakId") String keycloakId);

    /**
     * Recherche une entité complète par utilisateur et ticker.
     */
    Optional<WatchlistItemJpaEntity> findByKeycloakIdAndTicker(String keycloakId, String ticker);

    /**
     * Vérifie l'existence d'un ticker dans la watchlist d'un utilisateur.
     */
    boolean existsByKeycloakIdAndTicker(String keycloakId, String ticker);

    /**
     * Supprime un ticker spécifique.
     * Utilise @Modifying pour exécuter un DELETE direct en base sans SELECT préalable.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WatchlistItemJpaEntity w WHERE w.keycloakId = :keycloakId AND w.ticker = :ticker")
    void deleteByKeycloakIdAndTicker(@Param("keycloakId") String keycloakId, @Param("ticker") String ticker);

    /**
     * Supprime toute la watchlist d'un utilisateur.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WatchlistItemJpaEntity w WHERE w.keycloakId = :keycloakId")
    void deleteAllByKeycloakId(@Param("keycloakId") String keycloakId);
}
