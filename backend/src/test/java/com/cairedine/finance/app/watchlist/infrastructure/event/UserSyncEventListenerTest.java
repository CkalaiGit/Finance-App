package com.cairedine.finance.app.watchlist.infrastructure.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;

import com.cairedine.finance.app.user.UserSyncedEvent;

@ExtendWith(MockitoExtension.class)
class UserSyncEventListenerTest {

    private final UserSyncEventListener listener = new UserSyncEventListener();

    @Test
    void whenNewUser_logsInfo() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserSyncEventListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);

        listener.onUserSynced(new UserSyncedEvent("user-1", "email@example.com", "username", true));

        assertThat(listAppender.list).isNotEmpty();
        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(listAppender.list.getFirst().getFormattedMessage()).contains("user-1");

        logger.detachAppender(listAppender);
    }

    @Test
    void whenExistingUser_logsDebug() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserSyncEventListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        listener.onUserSynced(new UserSyncedEvent("user-2", "email@example.com", "username", false));

        assertThat(listAppender.list).isNotEmpty();
        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.DEBUG);
        assertThat(listAppender.list.getFirst().getFormattedMessage()).contains("user-2");

        logger.detachAppender(listAppender);
    }
}
