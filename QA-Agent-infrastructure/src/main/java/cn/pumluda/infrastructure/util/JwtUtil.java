package cn.pumluda.infrastructure.util;

import cn.pumluda.domain.identity.adapter.service.IJwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil implements IJwtTokenProvider {

    private final SecretKey key;
    private final long accessExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long accessExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
    }

    /**
     * 生成 access token
     */
    public String generateToken(String userId, String username, String role) {
        return Jwts.builder().subject(userId).claim("username", username).claim("role", role).issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + accessExpiration)).signWith(key).compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
