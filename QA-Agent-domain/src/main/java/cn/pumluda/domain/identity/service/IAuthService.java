package cn.pumluda.domain.identity.service;

import java.util.Map;

public interface IAuthService {

    /** 注册 */
    void register(String username, String password, String email);

    /** 登录 → 返回 token + 用户信息 */
    Map<String, Object> login(String username, String password);

}
