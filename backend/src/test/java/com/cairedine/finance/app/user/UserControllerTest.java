package com.cairedine.finance.app.user;

import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.security.SecurityConfig;
import com.cairedine.finance.app.user.infrastructure.security.WebConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test du UserController avec @WebMvcTest.
 * Charge uniquement la couche web — pas de JPA, pas de Keycloak réel.
 * On construit manuellement le JwtAuthenticationToken avec le UserContext en details
 * pour simuler exactement ce que KeycloakJwtConverter produit en vrai.
 * IUserSyncService est mocké car requis par KeycloakJwtConverter importé via SecurityConfig.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebConfig.class})
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserSyncService userSyncService;

    private JwtAuthenticationToken buildAuthToken(UserContext ctx) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(ctx.id())
                .claim("email", ctx.email())
                .claim("preferred_username", ctx.username())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authToken = new JwtAuthenticationToken(jwt, List.of());
        authToken.setDetails(ctx);
        return authToken;
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetMe {

        @Test
        @DisplayName("doit retourner 200 avec le UserContext quand JWT valide")
        void doitRetourner200AvecUserContext() throws Exception {
            var ctx = new UserContext("uuid-alice", "alice@finance.com", "alice", Set.of("PREMIUM"));

            mockMvc.perform(get("/api/users/me")
                            .with(authentication(buildAuthToken(ctx))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("uuid-alice"))
                    .andExpect(jsonPath("$.email").value("alice@finance.com"))
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("doit retourner 401 sans JWT")
        void doitRetourner401SansJwt() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}