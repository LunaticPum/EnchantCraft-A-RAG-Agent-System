package cn.pumluda.domain.bagu.model.entity;

public class QaItemEntity {
    private String id;
    private String setId;
    private String question;
    private String answer;
    private String difficulty;
    private int sortOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
