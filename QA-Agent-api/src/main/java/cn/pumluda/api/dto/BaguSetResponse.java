package cn.pumluda.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BaguSetResponse {
    private String id;
    private String title;
    private String description;
    private int itemCount;
    private List<BaguItemResponse> items;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public List<BaguItemResponse> getItems() { return items; }
    public void setItems(List<BaguItemResponse> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
