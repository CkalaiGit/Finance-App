package com.cairedine.finance.app.user.infrastructure.persistence.adapter;


import com.cairedine.finance.app.user.domain.model.User;
import com.cairedine.finance.app.user.domain.port.IUserRepositoryPort;
import com.cairedine.finance.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.cairedine.finance.app.user.infrastructure.persistence.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements IUserRepositoryPort {

    private final IUserRepository userRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(String keycloakId) {
        return userRepository.findById(keycloakId)
                .map(mapper::toDomain);
    }

    @Override
    public void save(User user) {
        userRepository.findById(user.keycloakId())
                .ifPresentOrElse(
                        entity -> {
                            entity.updateFrom(
                                    user.email(),
                                    user.username(),
                                    user.roles()
                            );
                            userRepository.save(entity);
                        },
                        () -> userRepository.save(mapper.toEntity(user))
                );
    }
}

