package com.taskmanager.database;

import com.taskmanager.model.Task;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private final Connection conn;

    public TaskDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // 创建任务，返回自增 id（后续要记录历史）
    public int insertTask(Task task) {
        String sql = """
            INSERT INTO tasks (title, description, status, priority, creator_id, assignee_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.setString(4, task.getPriority());
            ps.setInt(5, task.getCreatorId());
            ps.setInt(6, task.getAssigneeId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 查询所有任务
    public List<Task> getAllTasks() {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 查询分配给某用户的任务
    public List<Task> getTasksByAssignee(int assigneeId) {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE assignee_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assigneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 更新状态
    public boolean updateStatus(int taskId, String newStatus) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 更新分配人
    public boolean updateAssignee(int taskId, int assigneeId) {
        String sql = "UPDATE tasks SET assignee_id = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assigneeId);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ResultSet → Task 对象
    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getString("priority"),
            rs.getInt("creator_id"),
            rs.getInt("assignee_id"),
            rs.getString("created_at")
        );
    }
 // 按 creator_id 查询
    public List<Task> getTasksByCreator(int creatorId) {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE creator_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, creatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 按 id 查单条
    public Task getById(int id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}