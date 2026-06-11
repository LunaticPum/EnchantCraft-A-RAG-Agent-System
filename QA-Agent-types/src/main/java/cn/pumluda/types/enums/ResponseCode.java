package cn.pumluda.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Project: QA-Agent-Pumluda
 * Description: 统一响应状态码枚举——所有 API 返回的错误码均在此定义
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    // ==================== 通用 ====================
    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),

    // ==================== 文档相关 4xxxx ====================
    DOCUMENT_NOT_FOUND("40001", "文档不存在"),
    DOCUMENT_UPLOAD_FAILED("40002", "文档上传失败"),
    DOCUMENT_TYPE_UNSUPPORTED("40003", "不支持的文档类型"),
    DOCUMENT_CONTENT_EMPTY("40004", "文档内容为空"),
    ;

    /** 错误码 */
    private String code;

    /** 错误描述 */
    private String info;

}
