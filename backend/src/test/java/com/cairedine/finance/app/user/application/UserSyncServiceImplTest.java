package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.cairedine.finance.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.cairedine.finance.app.user.infrastructure.persistence.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserSyncServiceImpl.class, UserRepositoryAdapter.class, UserPersistenceMapper.class})
@DisplayName("UserSyncServiceImpl")
class UserSyncServiceImplTest {

    @Autowired
    private IUserSyncService userSyncService;

    @Autowired
    private IUserRepository userRepository;

    private Jwt buildJwt(String sub, String email, String username, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", sub)
                .claim("email", email)
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Creation d'un nouvel utilisateur")
    class Creation {

        @Test
        @DisplayName("doit creer un utilisateur et retourner le bon UserContext")
        void doitCreerUserEtRetournerContext() {
            var jwt = buildJwt("uuid-alice", "alice@finance.com", "alice", List.of("PREMIUM"));

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.id()).isEqualTo("uuid-alice");
            assertThat(ctx.email()).isEqualTo("alice@finance.com");
            assertThat(ctx.username()).isEqualTo("alice");
            assertThat(ctx.roles()).containsExactlyInAnyOrder("PREMIUM");
            assertThat(userRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Mise a jour d'un utilisateur existant")
    class MiseAJour {

        @BeforeEach
        void setUp() {
            var jwt = buildJwt("uuid-alice", "old@finance.com", "old_alice", List.of("FREEMIUM"));
            userSyncService.syncAndBuildContext(jwt);
        }

        @Test
        @DisplayName("doit mettre a jour et retourner le UserContext mis a jour")
        void doitMettreAJourEtRetournerContextMisAJour() {
            var jwt = buildJwt("uuid-alice", "new@finance.com", "new_alice", List.of("PREMIUM", "FREEMIUM"));

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.email()).isEqualTo("new@finance.com");
            assertThat(ctx.username()).isEqualTo("new_alice");
            assertThat(ctx.roles()).containsExactlyInAnyOrder("PREMIUM", "FREEMIUM");
        }

        @Test
        @DisplayName("ne doit pas creer un doublon en base")
        void neDoisPasCreerDoublon() {
            var jwt = buildJwt("uuid-alice", "new@finance.com", "new_alice", List.of("PREMIUM"));

            userSyncService.syncAndBuildContext(jwt);

            assertThat(userRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Extraction des roles depuis le JWT")
    class ExtractionRoles {

        @Test
        @DisplayName("doit retourner roles vide si realm_access absent")
        void doitGererAbsenceRealmAccess() {
            var jwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .claim("sub", "uuid-norole")
                    .claim("email", "norole@finance.com")
                    .claim("preferred_username", "norole")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.roles()).isEmpty();
        }

        @Test
        @DisplayName("doit retourner roles vide si la liste est vide")
        void doitGererListeRolesVide() {
            var jwt = buildJwt("uuid-empty", "empty@finance.com", "empty", List.of());

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.roles()).isEmpty();
        }
    }
}