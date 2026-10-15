package com.cairedine.finance.app.user.infrastructure.security;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.model.UserClaims;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakJwtConverter implements Converter<@NonNull Jwt, JwtAuthenticationToken> {

    private final IUserSyncService userSyncService;

    @Override
    public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
        UserClaims claims = extractClaims(jwt);
        UserContext ctx = userSyncService.syncAndBuildContext(claims);

        Set<SimpleGrantedAuthority> authorities = ctx.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
        authentication.setDetails(ctx);

        return authentication;
    }

    private UserClaims extractClaims(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        Set<String> roles = extractRoles(jwt);
        return new UserClaims(keycloakId, email, username, roles);
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptySet();
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List)) {
            return Collections.emptySet();
        }
        return ((List<Object>) rolesObj).stream()
                .filter(r -> r instanceof String)
                .map(r -> (String) r)
                .collect(Collectors.toUnmodifiableSet());
    }
}