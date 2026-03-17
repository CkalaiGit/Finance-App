package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.model.User;
import com.cairedine.finance.app.user.domain.port.IUserRepositoryPort;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncServiceImpl implements IUserSyncService {


    private final IUserRepositoryPort userRepository;

    @Override
    @Transactional
    public UserContext syncAndBuildContext(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email      = jwt.getClaimAsString("email");
        String username   = jwt.getClaimAsString("preferred_username");
        Set<String> roles = extractRoles(jwt);

        User user = userRepository.findById(keycloakId)
                .map(existing -> {
                    log.debug("Utilisateur mis à jour : {}", keycloakId);
                    return existing.updateFrom(email, username, roles);
                })
                .orElseGet(() -> {
                    log.info("Nouvel utilisateur : {}", keycloakId);
                    return new User(keycloakId, email, username, roles);
                });

        userRepository.save(user);

        return new UserContext(
                user.keycloakId(),
                user.email(),
                user.username(),
                user.roles()
        );
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