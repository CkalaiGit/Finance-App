package com.cairedine.finance.app.user.domain.port;

import com.cairedine.finance.app.user.domain.model.User;
import java.util.Optional;
import java.util.Set;

public interface IUserRepositoryPort {
    Optional<User> findById(String keycloakId);
    void save(User user);
    void addToWatchlist(String keycloakId, String ticker);
    void removeFromWatchlist(String keycloakId, String ticker);
    Set<String> getWatchlist(String keycloakId);
}
