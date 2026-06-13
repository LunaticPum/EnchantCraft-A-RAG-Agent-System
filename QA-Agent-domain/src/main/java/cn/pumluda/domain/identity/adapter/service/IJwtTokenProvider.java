package cn.pumluda.domain.identity.adapter.service;

public interface IJwtTokenProvider {

    String generateToken(String userId, String username, String role);

}
