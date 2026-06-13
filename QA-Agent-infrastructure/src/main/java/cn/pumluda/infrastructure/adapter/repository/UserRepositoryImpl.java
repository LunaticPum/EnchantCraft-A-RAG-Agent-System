package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.identity.adapter.repository.IUserRepository;
import cn.pumluda.domain.identity.model.entity.UserAccountEntity;
import cn.pumluda.infrastructure.dao.UserAccountDao;
import cn.pumluda.infrastructure.dao.po.UserAccountPO;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
    public void checkAndIncrementSearch(String userId, String role) {
        if ("ADMIN".equals(role)) return;
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po == null) return;
        resetIfNewDay(po);
        if (po.getSearchCount() >= 10) throw new AppException(ResponseCode.QUOTA_SEARCH_EXCEEDED.getCode(), "今日检索已达上限(10次)");
        searchCountUp(po);
        userAccountDao.updateById(po);
    }

    @Override
    public void checkAndIncrementChat(String userId, String role) {
        if ("ADMIN".equals(role)) return;
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po == null) return;
        resetIfNewDay(po);
        if (po.getChatCount() >= 10) throw new AppException(ResponseCode.QUOTA_CHAT_EXCEEDED.getCode(), "今日对话已达上限(10次)");
        chatCountUp(po);
        userAccountDao.updateById(po);
    }

    @Override
    public int getSearchRemaining(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po == null) return 0;
        resetIfNewDay(po);
        return Math.max(0, 10 - (po.getSearchCount() != null ? po.getSearchCount() : 0));
    }

    @Override
    public int getChatRemaining(String userId) {
        UserAccountPO po = userAccountDao.selectById(userId);
        if (po == null) return 0;
        resetIfNewDay(po);
        return Math.max(0, 10 - (po.getChatCount() != null ? po.getChatCount() : 0));
    }

    private void resetIfNewDay(UserAccountPO po) {
        LocalDate today = LocalDate.now();
        if (!today.equals(po.getLastReset())) {
            searchCountUp(po);  // trigger update via setter is empty here
        }
        // simpler inline
        if (!today.equals(po.getLastReset()) && (po.getSearchCount() != null && po.getSearchCount() > 0 || po.getChatCount() != null && po.getChatCount() > 0)) {
            po.setSearchCount(0);
            po.setChatCount(0);
            po.setLastReset(today);
        } else if (po.getLastReset() == null) {
            po.setLastReset(today);
            if (po.getSearchCount() == null) po.setSearchCount(0);
            if (po.getChatCount() == null) po.setChatCount(0);
        }
    }

    private void searchCountUp(UserAccountPO po) { po.setSearchCount((po.getSearchCount() == null ? 0 : po.getSearchCount()) + 1); }
    private void chatCountUp(UserAccountPO po) { po.setChatCount((po.getChatCount() == null ? 0 : po.getChatCount()) + 1); }

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
