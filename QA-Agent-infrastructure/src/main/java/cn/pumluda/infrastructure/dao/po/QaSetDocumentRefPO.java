package cn.pumluda.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("qa_set_document_ref")
public class QaSetDocumentRefPO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String setId;
    private String documentId;
}
