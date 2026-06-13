package cn.pumluda.domain.identity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountEntity {

    private String id;

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 邮箱 */
    private String email;

    /** 角色：ADMIN / USER */
    private String role;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
