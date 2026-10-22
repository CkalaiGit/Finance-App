package com.cairedine.finance.app.user.infrastructure.persistence.adapter;

import com.cairedine.finance.app.user.domain.model.User;
import com.cairedine.finance.app.user.infrastructure.persistence.entity.DBUserJpaEntity;
import com.cairedine.finance.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.cairedine.finance.app.user.infrastructure.persistence.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private IUserRepository userRepository;

    @Spy
    private UserPersistenceMapper mapper = new UserPersistenceMapper();

    @InjectMocks
    private UserRepositoryAdapter userRepositoryAdapter;

    private static final String KEYCLOAK_ID = "user-123";
    private static final String EMAIL = "test@example.com";
    private static final String USERNAME = "testuser";
    private static final Set<String> ROLES = Set.of("ROLE_USER");

    private DBUserJpaEntity userEntity;

    @BeforeEach
    void setUp() {
        userEntity = new DBUserJpaEntity(KEYCLOAK_ID, EMAIL, USERNAME, ROLES);
    }

    @Test
    void shouldFindByIdWhenUserExists() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(userEntity));

        // Act
        Optional<User> result = userRepositoryAdapter.findById(KEYCLOAK_ID);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().keycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(result.get().email()).isEqualTo(EMAIL);
        assertThat(result.get().username()).isEqualTo(USERNAME);
        assertThat(result.get().roles()).containsExactlyInAnyOrderElementsOf(ROLES);
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userRepositoryAdapter.findById(KEYCLOAK_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreateNewUserOnSaveWhenUserDoesNotExist() {
        // Arrange
        User user = new User(KEYCLOAK_ID, EMAIL, USERNAME, ROLES);
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.empty());

        // Act
        userRepositoryAdapter.save(user);

        // Assert
        ArgumentCaptor<DBUserJpaEntity> entityCaptor = ArgumentCaptor.forClass(DBUserJpaEntity.class);
        verify(userRepository).save(entityCaptor.capture());
        
        DBUserJpaEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(savedEntity.getEmail()).isEqualTo(EMAIL);
        assertThat(savedEntity.getUsername()).isEqualTo(USERNAME);
        assertThat(savedEntity.getRoles()).containsExactlyInAnyOrderElementsOf(ROLES);
    }

    @Test
    void shouldUpdateExistingUserOnSaveWhenUserExists() {
        // Arrange
        User user = new User(KEYCLOAK_ID, "newemail@example.com", "newusername", Set.of("ROLE_ADMIN"));
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(userEntity));

        // Act
        userRepositoryAdapter.save(user);

        // Assert
        verify(userRepository).save(userEntity);
        assertThat(userEntity.getEmail()).isEqualTo("newemail@example.com");
        assertThat(userEntity.getUsername()).isEqualTo("newusername");
        assertThat(userEntity.getRoles()).containsExactly("ROLE_ADMIN");
    }
}
