package com.cairedine.finance.app.user.infrastructure.persistence.repository;

import com.cairedine.finance.app.user.infrastructure.persistence.entity.DBUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface IUserRepository extends JpaRepository< @NonNull DBUserJpaEntity, @NonNull String> {
    Optional<DBUserJpaEntity> findByEmail(@NonNull String email);
}


