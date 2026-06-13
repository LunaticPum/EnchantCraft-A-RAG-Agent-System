package cn.pumluda.domain.identity.adapter.repository;

import cn.pumluda.domain.identity.model.entity.UserAccountEntity;

import java.util.Optional;

public interface IUserRepository {

    Optional<UserAccountEntity> findByUsername(String username);

    UserAccountEntity save(UserAccountEntity entity);

    boolean existsByUsername(String username);

    /** 检查配额并自增（含跨天重置），超限抛 AppException */
    void checkAndIncrementSearch(String userId, String role);

    void checkAndIncrementChat(String userId, String role);

    /** 查询剩余配额 */
    int getSearchRemaining(String userId);

    int getChatRemaining(String userId);

}
