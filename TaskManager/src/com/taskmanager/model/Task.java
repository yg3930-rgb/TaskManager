package com.taskmanager.model;

public class Task {
    // 状态常量，整个项目统一用这些字符串
    public static final String STATUS_TODO       = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE        = "DONE";

    // 优先级常量
    public static final String PRIORITY_LOW    = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH   = "HIGH";

    private int id;
    private String title;
    private String description;
    private String status;      // TODO / IN_PROGRESS / DONE
    private String priority;    // LOW / MEDIUM / HIGH
    private int creatorId;      // 创建人 user_id
    private int assigneeId;     // 被分配人 user_id（0表示未分配）
    private String createdAt;   // 创建时间

    public Task() {
        this.status = STATUS_TODO;
        this.priority = PRIORITY_MEDIUM;
    }

    public Task(int id, String title, String description,
                String status, String priority,
                int creatorId, int assigneeId, String createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.creatorId = creatorId;
        this.assigneeId = assigneeId;
        this.createdAt = createdAt;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public int getAssigneeId() { return assigneeId; }
    public void setAssigneeId(int assigneeId) { this.assigneeId = assigneeId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return "[" + id + "] " + title + " (" + status + ")"; }
}