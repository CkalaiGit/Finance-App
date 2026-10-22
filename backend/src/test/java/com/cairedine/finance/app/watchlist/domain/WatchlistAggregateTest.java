package com.cairedine.finance.app.watchlist.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WatchlistAggregate Tests")
class WatchlistAggregateTest {

    private static final String KEYCLOAK_ID = "user-123";
    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_MSFT = "MSFT";

    @Test
    @DisplayName("Devrait créer une watchlist vide")
    void shouldCreateEmptyWatchlist() {
        // Act
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID);

        // Assert
        assertThat(watchlist.keycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(watchlist.tickers()).isEmpty();
    }

    @Test
    @DisplayName("Devrait lever une exception si keycloakId est null")
    void shouldThrowExceptionIfKeycloakIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new WatchlistAggregate(null, new HashSet<>()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("keycloakId");
    }

    @Test
    @DisplayName("Devrait ajouter un ticker à la watchlist")
    void shouldAddTickerToWatchlist() {
        // Arrange
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID);

        // Act
        WatchlistAggregate updated = watchlist.addTicker(TICKER_AAPL);

        // Assert
        assertThat(updated.tickers()).contains(TICKER_AAPL);
        assertThat(watchlist.tickers()).isEmpty(); // Original immutable
    }

    @Test
    @DisplayName("Devrait convertir le ticker en majuscules")
    void shouldConvertTickerToUpperCase() {
        // Arrange
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID);

        // Act
        WatchlistAggregate updated = watchlist.addTicker("aapl");

        // Assert
        assertThat(updated.tickers()).contains("AAPL");
    }

    @Test
    @DisplayName("Devrait lever une exception si le ticker est vide")
    void shouldThrowExceptionIfTickerIsEmpty() {
        // Arrange
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID);

        // Act & Assert
        assertThatThrownBy(() -> watchlist.addTicker(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ticker");
    }

    @Test
    @DisplayName("Devrait supprimer un ticker de la watchlist")
    void shouldRemoveTickerFromWatchlist() {
        // Arrange
        Set<String> tickers = new HashSet<>(Set.of(TICKER_AAPL, TICKER_MSFT));
        WatchlistAggregate watchlist = new WatchlistAggregate(KEYCLOAK_ID, tickers);

        // Act
        WatchlistAggregate updated = watchlist.removeTicker(TICKER_AAPL);

        // Assert
        assertThat(updated.tickers()).contains(TICKER_MSFT);
        assertThat(updated.tickers()).doesNotContain(TICKER_AAPL);
    }

    @Test
    @DisplayName("Devrait vérifier la présence d'un ticker")
    void shouldCheckIfTickerExists() {
        // Arrange
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID)
            .addTicker(TICKER_AAPL);

        // Act & Assert
        assertThat(watchlist.containsTicker(TICKER_AAPL)).isTrue();
        assertThat(watchlist.containsTicker(TICKER_MSFT)).isFalse();
    }

    @Test
    @DisplayName("Devrait retourner false si le ticker est null")
    void shouldReturnFalseIfTickerIsNull() {
        // Arrange
        WatchlistAggregate watchlist = WatchlistAggregate.empty(KEYCLOAK_ID);

        // Act & Assert
        assertThat(watchlist.containsTicker(null)).isFalse();
    }

}

