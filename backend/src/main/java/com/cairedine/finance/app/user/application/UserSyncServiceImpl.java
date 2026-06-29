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
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserContext syncAndBuildContext(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email      = jwt.getClaimAsString("email");
        String username   = jwt.getClaimAsString("preferred_username");
        Set<String> roles = extractRoles(jwt);

        var existingOpt = userRepository.findById(keycloakId);
        User user;
        boolean isNewUser;
        if (existingOpt.isPresent()) {
            log.debug("Utilisateur mis à jour : {}", keycloakId);
            user = existingOpt.get().updateFrom(email, username, roles);
            isNewUser = false;
        } else {
            log.info("Nouvel utilisateur : {}", keycloakId);
            user = new User(keycloakId, email, username, roles);
            isNewUser = true;
        }

        userRepository.save(user);

        // Publier l'événement de domaine après la sauvegarde
        eventPublisher.publishEvent(new com.cairedine.finance.app.user.UserSyncedEvent(keycloakId, email, username, isNewUser));

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