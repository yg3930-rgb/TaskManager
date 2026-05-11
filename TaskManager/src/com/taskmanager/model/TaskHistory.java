package com.taskmanager.model;

public class TaskHistory {
    private int id;
    private int taskId;
    private int operatorId;     // 操作人
    private String action;      // 描述做了什么，如 "状态改为 DONE"
    private String changedAt;   // 操作时间

    public TaskHistory() {}

    public TaskHistory(int taskId, int operatorId, String action) {
        this.taskId = taskId;
        this.operatorId = operatorId;
        this.action = action;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getOperatorId() { return operatorId; }
    public void setOperatorId(int operatorId) { this.operatorId = operatorId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getChangedAt() { return changedAt; }
    public void setChangedAt(String changedAt) { this.changedAt = changedAt; }
}