package com.taskmanager.server;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;
import com.taskmanager.database.*;
import com.taskmanager.model.*;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class TaskService {

    private final UserDAO        userDAO    = new UserDAO();
    private final TaskDAO        taskDAO    = new TaskDAO();
    private final TaskHistoryDAO historyDAO = new TaskHistoryDAO();

    // Write lock to prevent race conditions when multiple clients modify shared data
    private static final ReentrantLock writeLock = new ReentrantLock();

    // ── Login ──
    // data format: "username|||password"
    public Message login(String data) {
        String[] parts = data.split("\\|\\|\\|");
        if (parts.length < 2) return error("Invalid parameters");

        User user = userDAO.login(parts[0], parts[1]);
        if (user == null) return error("Invalid username or password");

        // Returns "id|||username"
        return new Message(MessageType.LOGIN_SUCCESS,
                           user.getId() + "|||" + user.getUsername());
    }

    // ── Register ──
    // data format: "username|||password"
    public Message register(String data) {
        String[] parts = data.split("\\|\\|\\|");
        if (parts.length < 2) return error("Invalid parameters");

        boolean ok = userDAO.insertUser(new User(0, parts[0], parts[1]));
        return ok ? new Message(MessageType.SUCCESS, "Registration successful")
                  : error("Username already exists");
    }

    // ── Get all tasks ──
    public Message getAllTasks() {
        List<Task> tasks = taskDAO.getAllTasks();
        return new Message(MessageType.TASK_LIST, serializeTasks(tasks));
    }

    // ── Get tasks assigned to a specific user ──
    // data format: "userId"
    public Message getMyTasks(String data) {
        int userId = Integer.parseInt(data.trim());
        List<Task> tasks = taskDAO.getTasksByAssignee(userId);
        return new Message(MessageType.TASK_LIST, serializeTasks(tasks));
    }

    // ── Create task ──
    // data format: "title|||description|||priority|||creatorId|||assigneeId"
    public Message createTask(String data) {
        String[] p = data.split("\\|\\|\\|", -1);
        if (p.length < 5) return error("Invalid parameters");

        Task task = new Task();
        task.setTitle(p[0]);
        task.setDescription(p[1]);
        task.setPriority(p[2]);
        task.setCreatorId(Integer.parseInt(p[3]));
        task.setAssigneeId(Integer.parseInt(p[4]));

        writeLock.lock();
        try {
            int id = taskDAO.insertTask(task);
            if (id < 0) return error("Failed to create task");

            historyDAO.insert(new TaskHistory(id,
                task.getCreatorId(), "Created task"));
            if (task.getAssigneeId() != 0) {
                historyDAO.insert(new TaskHistory(id,
                    task.getCreatorId(),
                    "Assigned to user ID " + task.getAssigneeId()));
            }
            return new Message(MessageType.SUCCESS, String.valueOf(id));
        } finally {
            writeLock.unlock();
        }
    }

    // ── Update status ──
    // data format: "taskId|||newStatus|||operatorId"
    public Message updateStatus(String data) {
        String[] p = data.split("\\|\\|\\|");
        if (p.length < 3) return error("Invalid parameters");

        int taskId     = Integer.parseInt(p[0]);
        String status  = p[1];
        int operatorId = Integer.parseInt(p[2]);

        writeLock.lock();
        try {
            boolean ok = taskDAO.updateStatus(taskId, status);
            if (!ok) return error("Failed to update status");

            historyDAO.insert(new TaskHistory(taskId, operatorId,
                "Status changed to " + status));
            return new Message(MessageType.SUCCESS, "Status updated");
        } finally {
            writeLock.unlock();
        }
    }

    // ── Reassign task ──
    // data format: "taskId|||assigneeId|||operatorId"
    public Message updateAssignee(String data) {
        String[] p = data.split("\\|\\|\\|");
        if (p.length < 3) return error("Invalid parameters");

        int taskId     = Integer.parseInt(p[0]);
        int assigneeId = Integer.parseInt(p[1]);
        int operatorId = Integer.parseInt(p[2]);

        writeLock.lock();
        try {
            boolean ok = taskDAO.updateAssignee(taskId, assigneeId);
            if (!ok) return error("Failed to reassign task");

            historyDAO.insert(new TaskHistory(taskId, operatorId,
                "Reassigned to user ID " + assigneeId));
            return new Message(MessageType.SUCCESS, "Assignee updated");
        } finally {
            writeLock.unlock();
        }
    }

    // ── Get task history ──
    // data format: "taskId"
    public Message getHistory(String data) {
        int taskId = Integer.parseInt(data.trim());
        List<TaskHistory> list = historyDAO.getByTaskId(taskId);
        StringBuilder sb = new StringBuilder();
        for (TaskHistory h : list) {
            sb.append(h.getId()).append("~")
              .append(h.getTaskId()).append("~")
              .append(h.getOperatorId()).append("~")
              .append(h.getAction()).append("~")
              .append(h.getChangedAt()).append(";");
        }
        return new Message(MessageType.HISTORY_LIST, sb.toString());
    }

    // ── Get all users ──
    public Message getAllUsers() {
        List<User> users = userDAO.getAllUsers();
        StringBuilder sb = new StringBuilder();
        for (User u : users) {
            sb.append(u.getId()).append("~")
              .append(u.getUsername()).append(";");
        }
        return new Message(MessageType.USER_LIST, sb.toString());
    }

    // ── Helpers ──
    private Message error(String msg) {
        return new Message(MessageType.ERROR, msg);
    }

    // Serialize task list: tasks separated by ";", fields separated by "~"
    private String serializeTasks(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        for (Task t : tasks) {
            sb.append(t.getId()).append("~")
              .append(t.getTitle()).append("~")
              .append(t.getDescription() == null ? "" : t.getDescription()).append("~")
              .append(t.getStatus()).append("~")
              .append(t.getPriority()).append("~")
              .append(t.getCreatorId()).append("~")
              .append(t.getAssigneeId()).append("~")
              .append(t.getCreatedAt()).append(";");
        }
        return sb.toString();
    }
}