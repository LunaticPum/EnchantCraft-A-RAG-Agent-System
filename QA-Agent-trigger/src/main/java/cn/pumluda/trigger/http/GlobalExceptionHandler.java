package cn.pumluda.trigger.http;

import cn.pumluda.api.response.Response;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Project: QA-Agent-Pumluda
 * Description: 全局异常处理器——将所有异常统一包装为 Response 格式返回，避免前端收到非标准错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：使用 AppException 中携带的 code 和 message
     */
    @ExceptionHandler(AppException.class)
    public Response<Void> handleAppException(AppException e) {
        log.warn("[异常处理] 业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Response.<Void>builder()
                       .code(e.getCode())
                       .info(e.getMessage())
                       .build();
    }

    /**
     * 未知异常：兜底处理，返回统一错误码，避免暴露内部堆栈细节
     */
    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        log.error("[异常处理] 未知异常: {}", e.getMessage(), e);
        return Response.<Void>builder()
                       .code(ResponseCode.UN_ERROR.getCode())
                       .info("服务器内部错误: " + e.getMessage())
                       .build();
    }

}
