package com.cairedine.finance.app.user.domain.port;

import com.cairedine.finance.app.user.domain.model.User;
import java.util.Optional;

public interface IUserRepositoryPort {
    Optional<User> findById(String keycloakId);
    void save(User user);
}
