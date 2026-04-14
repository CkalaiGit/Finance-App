package com.cairedine.finance.app.user.infrastructure.persistence.adapter;

import com.cairedine.finance.app.user.infrastructure.persistence.entity.DBUserJpaEntity;
import com.cairedine.finance.app.user.infrastructure.persistence.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserRepositoryAdapter userRepositoryAdapter;

    private static final String KEYCLOAK_ID = "user-123";
    private DBUserJpaEntity userEntity;

    @BeforeEach
    void setUp() {
        userEntity = new DBUserJpaEntity();
        userEntity.setKeycloakId(KEYCLOAK_ID);
        userEntity.setWatchlist(new HashSet<>(Set.of("AAPL")));
    }

    @Test
    void shouldAddToWatchlist() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(userEntity));

        // Act
        userRepositoryAdapter.addToWatchlist(KEYCLOAK_ID, "msft");

        // Assert
        assertThat(userEntity.getWatchlist()).containsExactlyInAnyOrder("AAPL", "MSFT");
        verify(userRepository).save(userEntity);
    }

    @Test
    void shouldRemoveFromWatchlist() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(userEntity));

        // Act
        userRepositoryAdapter.removeFromWatchlist(KEYCLOAK_ID, "AAPL");

        // Assert
        assertThat(userEntity.getWatchlist()).isEmpty();
        verify(userRepository).save(userEntity);
    }

    @Test
    void shouldGetWatchlist() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(userEntity));

        // Act
        Set<String> result = userRepositoryAdapter.getWatchlist(KEYCLOAK_ID);

        // Assert
        assertThat(result).containsExactly("AAPL");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnEmptySetWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.empty());

        // Act
        Set<String> result = userRepositoryAdapter.getWatchlist(KEYCLOAK_ID);

        // Assert
        assertThat(result).isEmpty();
    }
}
