package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.cairedine.finance.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.cairedine.finance.app.user.infrastructure.persistence.repository.IUserRepository;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import java.util.ArrayList;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserSyncServiceImpl.class, UserRepositoryAdapter.class, UserPersistenceMapper.class, UserSyncServiceImplTest.TestConfig.class})
@DisplayName("UserSyncServiceImpl")
class UserSyncServiceImplTest {

    @Autowired
    private IUserSyncService userSyncService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private TestEventRecorder eventRecorder;

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestEventRecorder testEventRecorder() { return new TestEventRecorder(); }
    }

    @Getter
    public static class TestEventRecorder {
        private final List<Object> events = new ArrayList<>();
        @EventListener
        public void recordEvent(Object evt) { events.add(evt); }
    }

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

            var evts = eventRecorder.getEvents();
            Object last = evts.getLast();
            assertThat(last).isInstanceOf(com.cairedine.finance.app.user.UserSyncedEvent.class);
            com.cairedine.finance.app.user.UserSyncedEvent e = (com.cairedine.finance.app.user.UserSyncedEvent) last;
            assertThat(e.isNewUser()).isTrue();
            assertThat(e.keycloakId()).isEqualTo("uuid-alice");
            assertThat(e.email()).isEqualTo("alice@finance.com");
            assertThat(e.username()).isEqualTo("alice");
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

            var evts = eventRecorder.getEvents();
            Object last = evts.getLast();
            assertThat(last).isInstanceOf(com.cairedine.finance.app.user.UserSyncedEvent.class);
            com.cairedine.finance.app.user.UserSyncedEvent ue = (com.cairedine.finance.app.user.UserSyncedEvent) last;
            assertThat(ue.isNewUser()).isFalse();
            assertThat(ue.keycloakId()).isEqualTo("uuid-alice");
            assertThat(ue.email()).isEqualTo("new@finance.com");
            assertThat(ue.username()).isEqualTo("new_alice");

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