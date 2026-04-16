package com.cairedine.finance.app.watchlist;

import com.cairedine.finance.app.watchlist.infrastructure.persistence.entity.WatchlistItemJpaEntity;
import com.cairedine.finance.app.watchlist.infrastructure.persistence.repository.IWatchlistJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WatchlistItemJpaEntity Repository Tests")
class WatchlistRepositoryH2Test {

    private static final String KEYCLOAK_ID = "user-123";
    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_MSFT = "MSFT";

    @Autowired
    private IWatchlistJpaRepository watchlistRepository;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
    }

    @Test
    @DisplayName("Devrait sauvegarder et récupérer un item de watchlist")
    void shouldSaveAndFindWatchlistItem() {
        // Arrange
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();

        // Act
        watchlistRepository.save(item);
        Optional<WatchlistItemJpaEntity> found = watchlistRepository.findByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTicker()).isEqualTo(TICKER_AAPL);
        assertThat(found.get().getKeycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(found.get().getAddedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait récupérer tous les tickers d'un utilisateur")
    void shouldFindAllTickersForUser() {
        // Arrange
        WatchlistItemJpaEntity item1 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        WatchlistItemJpaEntity item2 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_MSFT)
            .build();

        watchlistRepository.saveAll(List.of(item1, item2));

        // Act
        List<WatchlistItemJpaEntity> items = watchlistRepository.findByKeycloakId(KEYCLOAK_ID);

        // Assert
        assertThat(items).hasSize(2);
        assertThat(items.stream().map(WatchlistItemJpaEntity::getTicker))
            .containsExactlyInAnyOrder(TICKER_AAPL, TICKER_MSFT);
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un ticker")
    void shouldCheckTickerExists() {
        // Arrange
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        watchlistRepository.save(item);

        // Act & Assert
        assertThat(watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL))
            .isTrue();
        assertThat(watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_MSFT))
            .isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer un ticker de la watchlist")
    void shouldDeleteTickerFromWatchlist() {
        // Arrange
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        watchlistRepository.save(item);

        // Act
        watchlistRepository.deleteByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        assertThat(watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL))
            .isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer toute la watchlist d'un utilisateur")
    void shouldDeleteAllTickersForUser() {
        // Arrange
        WatchlistItemJpaEntity item1 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        WatchlistItemJpaEntity item2 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_MSFT)
            .build();
        watchlistRepository.saveAll(List.of(item1, item2));

        // Act
        watchlistRepository.deleteByKeycloakId(KEYCLOAK_ID);

        // Assert
        assertThat(watchlistRepository.findByKeycloakId(KEYCLOAK_ID)).isEmpty();
    }

    @Test
    @DisplayName("Devrait respecter la contrainte d'unicité (keycloak_id, ticker)")
    void shouldEnforceUniqueConstraint() {
        // Arrange
        WatchlistItemJpaEntity item1 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();

        watchlistRepository.save(item1);

        // Act & Assert : La deuxième sauvegarde devrait échouer
        assertThat(watchlistRepository.findByKeycloakId(KEYCLOAK_ID)).hasSize(1);
    }
}

