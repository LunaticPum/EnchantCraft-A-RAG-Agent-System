package cn.pumluda.api.dto;

import lombok.Data;

/**
 * Project: QA-Agent-Pumluda <p>
 * File: ChatRequest <p>
 * Created by: 16374 <p>
 * Date: 2026/6/10 <p>
 * Time: 11:03 <p>
 * Description: 对话请求 DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户问题
     */
    private String message;

}
