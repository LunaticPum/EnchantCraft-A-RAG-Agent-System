package cn.pumluda.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project: QA-Agent-Pumluda
 * Description: message_job 表持久化对象——Kafka 异步索引消息追踪
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_job")
public class MessageJobPO {

    /** 消息唯一标识（UUID） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String jobId;

    /** 关联文档 ID */
    private String documentId;

    /** Kafka 主题名 */
    private String topic;

    /** 状态：PENDING / COMPLETED / FAILED */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最后一次失败的错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

}
