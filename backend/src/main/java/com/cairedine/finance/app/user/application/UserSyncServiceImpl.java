package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.entity.DBUser;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.repository.IUserRepository;
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

    private final IUserRepository userRepository;

    @Override
    @Transactional
    public UserContext syncAndBuildContext(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email      = jwt.getClaimAsString("email");
        String username   = jwt.getClaimAsString("preferred_username");
        Set<String> roles = extractRoles(jwt);

        DBUser user = userRepository.findById(keycloakId)
                .map(existing -> {
                    existing.updateFrom(email, username, roles);
                    log.debug("Utilisateur mis à jour : {}", keycloakId);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Nouvel utilisateur : {}", keycloakId);
                    return new DBUser(keycloakId, email, username, roles);
                });

        userRepository.save(user);

        return new UserContext(keycloakId, email, username, roles);
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