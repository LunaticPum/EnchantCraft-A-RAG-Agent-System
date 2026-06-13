package cn.pumluda.domain.identity.adapter.repository;

import cn.pumluda.domain.identity.model.entity.UserAccountEntity;

import java.util.Optional;

public interface IUserRepository {

    Optional<UserAccountEntity> findByUsername(String username);

    UserAccountEntity save(UserAccountEntity entity);

    boolean existsByUsername(String username);

}
