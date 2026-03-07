package com.cairedine.finance.app.user.infrastructure.security;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakJwtConverter implements Converter<@NonNull Jwt, JwtAuthenticationToken> {

    private final IUserSyncService userSyncService;

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        UserContext ctx = userSyncService.syncAndBuildContext(jwt);

        Set<SimpleGrantedAuthority> authorities = ctx.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
        authentication.setDetails(ctx);

        return authentication;
    }
}