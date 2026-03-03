package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.entity.DBUser;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.repository.IUserRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de UserSyncServiceImpl.
 * On utilise @DataJpaTest + le vrai repository H2.
 * Le service est importé via @Import — pas de mock de repository.
 * On teste la logique métier : création, mise à jour, extraction des rôles.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(UserSyncServiceImpl.class)
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
    @DisplayName("Création d'un nouvel utilisateur")
    class Creation {

        @Test
        @DisplayName("doit créer un DBUser quand le sub est inconnu")
        void doitCreerUserQuandSubInconnu() {
            var jwt = buildJwt("uuid-alice", "alice@finance.com", "alice", List.of("PREMIUM"));

            userSyncService.syncAndBuildContext(jwt);

            var saved = userRepository.findById("uuid-alice");
            assertThat(saved).isPresent();
            assertThat(saved.get().getEmail()).isEqualTo("alice@finance.com");
            assertThat(saved.get().getRoles()).containsExactlyInAnyOrder("PREMIUM");
        }

        @Test
        @DisplayName("doit retourner un UserContext avec les bonnes valeurs")
        void doitRetournerUserContextCorrect() {
            var jwt = buildJwt("uuid-alice", "alice@finance.com", "alice", List.of("PREMIUM"));

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.id()).isEqualTo("uuid-alice");
            assertThat(ctx.email()).isEqualTo("alice@finance.com");
            assertThat(ctx.username()).isEqualTo("alice");
            assertThat(ctx.roles()).containsExactlyInAnyOrder("PREMIUM");
        }
    }

    @Nested
    @DisplayName("Mise à jour d'un utilisateur existant")
    class MiseAJour {

        @BeforeEach
        void setUp() {
            userRepository.save(new DBUser("uuid-alice", "old@finance.com", "old_alice", Set.of("FREEMIUM")));
        }

        @Test
        @DisplayName("doit mettre à jour le DBUser quand le sub existe déjà")
        void doitMettreAJourUserExistant() {
            var jwt = buildJwt("uuid-alice", "new@finance.com", "new_alice", List.of("PREMIUM", "FREEMIUM"));

            userSyncService.syncAndBuildContext(jwt);

            var updated = userRepository.findById("uuid-alice");
            assertThat(updated).isPresent();
            assertThat(updated.get().getEmail()).isEqualTo("new@finance.com");
            assertThat(updated.get().getUsername()).isEqualTo("new_alice");
            assertThat(updated.get().getRoles()).containsExactlyInAnyOrder("PREMIUM", "FREEMIUM");
        }

        @Test
        @DisplayName("ne doit pas créer un doublon en base")
        void neDoisPasCreerDoublon() {
            var jwt = buildJwt("uuid-alice", "new@finance.com", "new_alice", List.of("PREMIUM"));

            userSyncService.syncAndBuildContext(jwt);

            assertThat(userRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Extraction des rôles depuis le JWT")
    class ExtractionRoles {

        @Test
        @DisplayName("doit retourner un UserContext avec roles vide si realm_access absent")
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
        @DisplayName("doit retourner un UserContext avec roles vide si la liste est vide")
        void doitGererListeRolesVide() {
            var jwt = buildJwt("uuid-empty", "empty@finance.com", "empty", List.of());

            UserContext ctx = userSyncService.syncAndBuildContext(jwt);

            assertThat(ctx.roles()).isEmpty();
        }
    }
}