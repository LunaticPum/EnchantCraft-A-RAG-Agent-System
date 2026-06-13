package cn.pumluda.domain.identity.adapter.repository;

import cn.pumluda.domain.identity.model.entity.UserAccountEntity;

import java.util.Optional;

public interface IUserRepository {

    Optional<UserAccountEntity> findByUsername(String username);

    UserAccountEntity save(UserAccountEntity entity);

    boolean existsByUsername(String username);

    void incrementSearchCount(String userId);

    void incrementChatCount(String userId);

    int getSearchCount(String userId);

    int getChatCount(String userId);

}
