package cn.pumluda.config;

import cn.pumluda.domain.identity.adapter.repository.IUserRepository;
import cn.pumluda.domain.identity.model.entity.UserAccountEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final IUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        if (userRepository.existsByUsername("Pumluda")) {
            log.info("[Admin初始化] 已存在，跳过");
            return;
        }
        userRepository.save(UserAccountEntity.builder()
                                             .username("Pumluda")
                                             .password(passwordEncoder.encode("pjp020910"))
                                             .email("admin@qa-agent.com")
                                             .role("ADMIN")
                                             .status(1)
                                             .build());
        log.info("[Admin初始化] 管理员已创建: username=Pumluda, role=ADMIN");
    }

}
