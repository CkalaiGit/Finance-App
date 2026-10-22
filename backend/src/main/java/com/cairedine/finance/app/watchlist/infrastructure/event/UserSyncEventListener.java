package com.cairedine.finance.app.watchlist.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.cairedine.finance.app.user.UserSyncedEvent;

@Component
public class UserSyncEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserSyncEventListener.class);

    @ApplicationModuleListener
    public void onUserSynced(UserSyncedEvent event) {
        if (event.isNewUser()) {
            log.info("User {} is ready for watchlist", event.keycloakId());
        } else {
            log.debug("User {} updated (email/username changes ignored)", event.keycloakId());
        }
    }
}
