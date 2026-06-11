package cn.pumluda.domain.document.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档类型值对象（枚举），定义系统支持的文档格式
 */
@Getter
@AllArgsConstructor
public enum DocumentType {

    /** Markdown 格式文档 */
    MARKDOWN("MARKDOWN", "Markdown 文档"),
    ;

    /** 类型编码，对应数据库 file_type 字段值 */
    private final String code;

    /** 中文描述 */
    private final String desc;

}
