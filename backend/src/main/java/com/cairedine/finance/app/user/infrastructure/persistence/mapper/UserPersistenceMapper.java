package com.cairedine.finance.app.user.infrastructure.persistence.mapper;

import com.cairedine.finance.app.user.domain.model.User;
import com.cairedine.finance.app.user.infrastructure.persistence.entity.DBUserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public User toDomain(DBUserJpaEntity entity) {
        return new User(
                entity.getKeycloakId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getRoles()
        );
    }

    public DBUserJpaEntity toEntity(User user) {
        return new DBUserJpaEntity(
                user.keycloakId(),
                user.email(),
                user.username(),
                user.roles()
        );
    }
}
