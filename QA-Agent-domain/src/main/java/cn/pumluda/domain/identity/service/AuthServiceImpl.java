package cn.pumluda.domain.identity.service;

import cn.pumluda.domain.identity.adapter.repository.IUserRepository;
import cn.pumluda.domain.identity.adapter.service.IJwtTokenProvider;
import cn.pumluda.domain.identity.model.entity.UserAccountEntity;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserRepository userRepository;
    private final IJwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ResponseCode.AUTH_USERNAME_EXISTS.getCode(), "用户名已存在");
        }
        UserAccountEntity entity = UserAccountEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role("USER")
                .status(1)
                .build();
        userRepository.save(entity);
        log.info("[认证] 注册成功: username={}", username);
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        UserAccountEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ResponseCode.AUTH_BAD_CREDENTIALS.getCode(), "用户名或密码错误"));

        if (user.getStatus() == 0) {
            throw new AppException(ResponseCode.AUTH_ACCOUNT_DISABLED.getCode(), "账户已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException(ResponseCode.AUTH_BAD_CREDENTIALS.getCode(), "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("[认证] 登录成功: username={}, role={}", username, user.getRole());

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        );
    }

}
