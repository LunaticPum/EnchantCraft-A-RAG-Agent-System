package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.identity.adapter.repository.IUserRepository;
import cn.pumluda.domain.identity.model.entity.UserAccountEntity;
import cn.pumluda.infrastructure.dao.UserAccountDao;
import cn.pumluda.infrastructure.dao.po.UserAccountPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements IUserRepository {

    private final UserAccountDao userAccountDao;

    @Override
    public Optional<UserAccountEntity> findByUsername(String username) {
        UserAccountPO po = userAccountDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccountPO>()
                        .eq(UserAccountPO::getUsername, username)
        );
        if (po == null) return Optional.empty();
        return Optional.of(toEntity(po));
    }

    @Override
    public UserAccountEntity save(UserAccountEntity entity) {
        UserAccountPO po = toPO(entity);
        if (entity.getId() == null || entity.getId().isBlank()) {
            userAccountDao.insert(po);
        } else {
            userAccountDao.updateById(po);
        }
        return toEntity(po);
    }

    @Override
    public void incrementSearchCount(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po != null) {
            po.setSearchCount((po.getSearchCount() == null ? 0 : po.getSearchCount()) + 1);
            userAccountDao.updateById(po);
        }
    }

    @Override
    public void incrementChatCount(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po != null) {
            po.setChatCount((po.getChatCount() == null ? 0 : po.getChatCount()) + 1);
            userAccountDao.updateById(po);
        }
    }

    @Override
    public int getSearchCount(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        return po != null && po.getSearchCount() != null ? po.getSearchCount() : 0;
    }

    @Override
    public int getChatCount(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        return po != null && po.getChatCount() != null ? po.getChatCount() : 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userAccountDao.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccountPO>()
                        .eq(UserAccountPO::getUsername, username)
        ) > 0;
    }

    private UserAccountPO toPO(UserAccountEntity e) {
        return UserAccountPO.builder()
                            .id(e.getId()).username(e.getUsername()).password(e.getPassword())
                            .email(e.getEmail()).role(e.getRole()).status(e.getStatus())
                            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }

    private UserAccountEntity toEntity(UserAccountPO po) {
        return UserAccountEntity.builder()
                                .id(po.getId()).username(po.getUsername()).password(po.getPassword())
                                .email(po.getEmail()).role(po.getRole()).status(po.getStatus())
                                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt()).build();
    }
}
