package cn.pumluda.api.dto;

import java.util.List;

public class BaguGenerateRequest {
    private String shelfName;
    private List<String> documentIds;

    public String getShelfName() { return shelfName; }
    public void setShelfName(String shelfName) { this.shelfName = shelfName; }
    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }
}
