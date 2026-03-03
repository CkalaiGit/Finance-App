package com.cairedine.finance.app.user.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DBUser")
class DBUserTest {

    private String keycloakId;
    private String email;
    private String username;
    private Set<String> roles;

    @BeforeEach
    void setUp() {
        keycloakId = "uuid-123";
        email      = "alice@finance.com";
        username   = "alice";
        roles      = new HashSet<>(Set.of("PREMIUM"));
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("doit initialiser createdAt et updatedAt automatiquement")
        void doitInitialiserLesTimestamps() {
            var before = Instant.now();
            var user   = new DBUser(keycloakId, email, username, roles);
            var after  = Instant.now();

            assertThat(user.getCreatedAt()).isBetween(before, after);
            assertThat(user.getUpdatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("doit retourner un ensemble vide si roles est null")
        void doitAccepterRolesNull() {
            var user = new DBUser(keycloakId, email, username, null);

            assertThat(user.getRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateFrom")
    class UpdateFrom {

        @Test
        @DisplayName("doit rafraîchir updatedAt sans modifier createdAt")
        void doitRafraichirUpdatedAtSansModifierCreatedAt() throws InterruptedException {
            var user      = new DBUser(keycloakId, email, username, roles);
            var createdAt = user.getCreatedAt();

            Thread.sleep(10);
            var before = Instant.now();
            user.updateFrom("new@finance.com", "alice_new", Set.of("FREEMIUM"));
            var after = Instant.now();

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
            assertThat(user.getUpdatedAt()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Immuabilité des rôles")
    class ImmuabiliteDesRoles {

        @Test
        @DisplayName("getRoles doit retourner une copie défensive")
        void getRoleDoitRetournerUneCopieDefensive() {
            var user = new DBUser(keycloakId, email, username, roles);
            var copy = user.getRoles();

            assertThatThrownBy(() -> copy.add("ADMIN"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("modifier le Set original ne doit pas affecter DBUser")
        void modifierSetOriginalNedoitPasAffecterDBUser() {
            var user = new DBUser(keycloakId, email, username, roles);

            roles.add("ADMIN");

            assertThat(user.getRoles()).containsExactly("PREMIUM");
        }
    }
}