package com.cairedine.finance.app.user.application;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.UserSyncedEvent;
import com.cairedine.finance.app.user.domain.model.User;
import com.cairedine.finance.app.user.domain.model.UserClaims;
import com.cairedine.finance.app.user.domain.port.IUserRepositoryPort;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncServiceImpl implements IUserSyncService {

    private final IUserRepositoryPort userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserContext syncAndBuildContext(UserClaims claims) {
        String keycloakId = claims.keycloakId();
        String email      = claims.email();
        String username   = claims.username();
        Set<String> roles = claims.roles();

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
        eventPublisher.publishEvent(new UserSyncedEvent(keycloakId, email, username, isNewUser));

        return new UserContext(
                user.keycloakId(),
                user.email(),
                user.username(),
                user.roles()
        );
    }
}