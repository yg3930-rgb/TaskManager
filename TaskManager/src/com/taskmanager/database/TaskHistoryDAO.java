package com.taskmanager.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.taskmanager.model.TaskHistory;
public class TaskHistoryDAO {

    private final Connection conn;

    public TaskHistoryDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // 插入一条历史记录
    public boolean insert(TaskHistory history) {
        String sql = "INSERT INTO task_history (task_id, operator_id, action) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, history.getTaskId());
            ps.setInt(2, history.getOperatorId());
            ps.setString(3, history.getAction());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 查询某个任务的所有历史（按时间升序）
    public List<TaskHistory> getByTaskId(int taskId) {
        List<TaskHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM task_history WHERE task_id = ? ORDER BY changed_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TaskHistory h = new TaskHistory();
                h.setId(rs.getInt("id"));
                h.setTaskId(rs.getInt("task_id"));
                h.setOperatorId(rs.getInt("operator_id"));
                h.setAction(rs.getString("action"));
                h.setChangedAt(rs.getString("changed_at"));
                list.add(h);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}