package com.cairedine.finance.app.user.infrastructure.repository;

import com.cairedine.finance.app.user.domain.entity.DBUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface IUserRepository extends JpaRepository< @NonNull DBUser, @NonNull String> {
    Optional<DBUser> findByEmail(@NonNull String email);
}


