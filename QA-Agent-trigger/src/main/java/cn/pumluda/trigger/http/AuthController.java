package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.AuthRequest;
import cn.pumluda.api.dto.AuthResponse;
import cn.pumluda.api.response.Response;
import cn.pumluda.domain.identity.service.IAuthService;
import cn.pumluda.types.enums.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    public Response<Void> register(@RequestBody AuthRequest req) {
        authService.register(req.getUsername(), req.getPassword(), req.getEmail());
        return Response.<Void>builder().code(ResponseCode.SUCCESS.getCode()).info("注册成功").build();
    }

    @PostMapping("/login")
    public Response<AuthResponse> login(@RequestBody AuthRequest req) {
        Map<String, Object> result = authService.login(req.getUsername(), req.getPassword());
        return Response.<AuthResponse>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info("登录成功")
                       .data(AuthResponse.builder()
                                         .token((String) result.get("token"))
                                         .userId((String) result.get("userId"))
                                         .username((String) result.get("username"))
                                         .role((String) result.get("role"))
                                         .build())
                       .build();
    }

}
