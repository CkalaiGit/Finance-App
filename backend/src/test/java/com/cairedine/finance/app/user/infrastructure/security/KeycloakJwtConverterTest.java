package com.cairedine.finance.app.user.infrastructure.security;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakJwtConverter")
class KeycloakJwtConverterTest {

    @Mock
    private IUserSyncService userSyncService;

    @InjectMocks
    private KeycloakJwtConverter keycloakJwtConverter;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "uuid-alice")
                .claim("email", "alice@finance.com")
                .claim("preferred_username", "alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("Conversion JWT → Authentication")
    class Conversion {

        @Test
        @DisplayName("doit retourner un JwtAuthenticationToken")
        void doitRetournerJwtAuthenticationToken() {
            var ctx = new UserContext("uuid-alice", "alice@finance.com", "alice", Set.of("PREMIUM"));
            when(userSyncService.syncAndBuildContext(jwt)).thenReturn(ctx);

            var result = keycloakJwtConverter.convert(jwt);

            assertThat(result).isInstanceOf(JwtAuthenticationToken.class);
        }

        @Test
        @DisplayName("doit attacher le UserContext en details")
        void doitAttacherUserContextEnDetails() {
            var ctx = new UserContext("uuid-alice", "alice@finance.com", "alice", Set.of("PREMIUM"));
            when(userSyncService.syncAndBuildContext(jwt)).thenReturn(ctx);

            var result = (JwtAuthenticationToken) keycloakJwtConverter.convert(jwt);

            assertThat(result.getDetails()).isEqualTo(ctx);
        }

        @Test
        @DisplayName("doit convertir les rôles en authorities ROLE_")
        void doitConvertirRolesEnAuthorities() {
            var ctx = new UserContext("uuid-alice", "alice@finance.com", "alice", Set.of("PREMIUM", "FREEMIUM"));
            when(userSyncService.syncAndBuildContext(jwt)).thenReturn(ctx);

            var result = (JwtAuthenticationToken) keycloakJwtConverter.convert(jwt);

            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_PREMIUM", "ROLE_FREEMIUM");
        }

        @Test
        @DisplayName("doit retourner des authorities vides si roles vide")
        void doitRetournerAuthoritiesVidesRolesVide() {
            var ctx = new UserContext("uuid-alice", "alice@finance.com", "alice", Set.of());
            when(userSyncService.syncAndBuildContext(jwt)).thenReturn(ctx);

            var result = (JwtAuthenticationToken) keycloakJwtConverter.convert(jwt);

            assertThat(result.getAuthorities()).isEmpty();
        }
    }
}