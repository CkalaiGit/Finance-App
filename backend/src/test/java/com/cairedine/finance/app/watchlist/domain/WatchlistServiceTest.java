package com.cairedine.finance.app.watchlist.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistService Tests")
class WatchlistServiceTest {

    private static final String KEYCLOAK_ID = "user-123";
    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_MSFT = "MSFT";

    @Mock
    private IWatchlistRepositoryPort watchlistRepository;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistRepository);
    }

    @Test
    @DisplayName("Devrait ajouter un ticker à la watchlist")
    void shouldAddTickerToWatchlist() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(false);

        // Act
        watchlistService.addTickerToWatchlist(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        verify(watchlistRepository).addTickerToWatchlist(KEYCLOAK_ID, TICKER_AAPL);
    }

    @Test
    @DisplayName("Devrait lever une exception si le ticker existe déjà")
    void shouldThrowExceptionIfTickerAlreadyExists() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> watchlistService.addTickerToWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("Devrait lever une exception si le ticker est vide")
    void shouldThrowExceptionIfTickerIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> watchlistService.addTickerToWatchlist(KEYCLOAK_ID, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Devrait supprimer un ticker de la watchlist")
    void shouldRemoveTickerFromWatchlist() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(true);

        // Act
        watchlistService.removeTickerFromWatchlist(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        verify(watchlistRepository).removeTickerFromWatchlist(KEYCLOAK_ID, TICKER_AAPL);
    }

    @Test
    @DisplayName("Devrait lever une exception si le ticker n'existe pas")
    void shouldThrowExceptionIfTickerNotFound() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> watchlistService.removeTickerFromWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("n'existe pas");
    }

    @Test
    @DisplayName("Devrait récupérer la watchlist de l'utilisateur")
    void shouldGetUserWatchlist() {
        // Arrange
        Set<String> expectedTickers = Set.of(TICKER_AAPL, TICKER_MSFT);
        when(watchlistRepository.findAllTickersByKeycloakId(KEYCLOAK_ID))
            .thenReturn(expectedTickers);

        // Act
        Set<String> result = watchlistService.getUserWatchlist(KEYCLOAK_ID);

        // Assert
        assertThat(result).containsExactlyInAnyOrder(TICKER_AAPL, TICKER_MSFT);
    }

    @Test
    @DisplayName("Devrait vérifier si un ticker est dans la watchlist")
    void shouldCheckIfTickerIsInWatchlist() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(true);

        // Act
        boolean result = watchlistService.isTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Devrait retourner false si le ticker n'est pas dans la watchlist")
    void shouldReturnFalseIfTickerNotInWatchlist() {
        // Arrange
        when(watchlistRepository.existsTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL))
            .thenReturn(false);

        // Act
        boolean result = watchlistService.isTickerInWatchlist(KEYCLOAK_ID, TICKER_AAPL);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Devrait retourner false si le ticker est null")
    void shouldReturnFalseIfTickerIsNull() {
        // Act
        boolean result = watchlistService.isTickerInWatchlist(KEYCLOAK_ID, null);

        // Assert
        assertThat(result).isFalse();
    }
}

