package com.taskmanager.client;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskHistory;
import com.taskmanager.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class TaskDetailDialog extends JDialog {

    private final int taskId;
    private final User currentUser;
    private final ClientSocket clientSocket;
    private final Runnable onUpdate;

    public TaskDetailDialog(JFrame parent, int taskId, User currentUser,
                            ClientSocket clientSocket, Runnable onUpdate) {
        super(parent, "Task Detail #" + taskId, true);
        this.taskId       = taskId;
        this.currentUser  = currentUser;
        this.clientSocket = clientSocket;
        this.onUpdate     = onUpdate;
        initUI();
    }

    private void initUI() {
        setSize(580, 420);
        setLocationRelativeTo(getParent());

        Task task = null;
        List<User> allUsers = null;
        try {
            Message taskResp = clientSocket.getAllTasks();
            List<Task> tasks = MainFrame.parseTasks(taskResp.getData());
            for (Task t : tasks) {
                if (t.getId() == taskId) { task = t; break; }
            }
            Message userResp = clientSocket.getAllUsers();
            allUsers = MainFrame.parseUsers(userResp.getData());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load task: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        if (task == null) { dispose(); return; }

        final Task finalTask   = task;
        final List<User> users = allUsers;

        String creatorName  = findUsername(users, task.getCreatorId());
        String assigneeName = task.getAssigneeId() == 0 ? "—"
            : findUsername(users, task.getAssigneeId());

        // ── Left: task info ──
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        String[][] rows = {
            {"Title",      task.getTitle()},
            {"Status",     task.getStatus()},
            {"Priority",   task.getPriority()},
            {"Creator",    creatorName},
            {"Assignee",   assigneeName},
            {"Created At", task.getCreatedAt()},
            {"Description", task.getDescription() == null ? "" : task.getDescription()},
        };
        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            infoPanel.add(new JLabel(rows[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            infoPanel.add(new JLabel(rows[i][1]), gbc);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton updateStatusBtn = new JButton("Update Status");
        JButton reassignBtn     = new JButton("Reassign");
        updateStatusBtn.addActionListener(e -> showUpdateStatusDialog(finalTask));
        reassignBtn    .addActionListener(e -> showReassignDialog(finalTask, users));
        btnPanel.add(updateStatusBtn);
        btnPanel.add(reassignBtn);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(infoPanel, BorderLayout.CENTER);
        leftPanel.add(btnPanel,  BorderLayout.SOUTH);

        // ── Right: history ──
        DefaultTableModel histModel = new DefaultTableModel(
            new String[]{"Time", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(histModel);
        histTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        histTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        histTable.setRowHeight(24);

        try {
            Message histResp = clientSocket.getHistory(taskId);
            List<TaskHistory> histories = parseHistories(histResp.getData(), users);
            for (TaskHistory h : histories) {
                histModel.addRow(new Object[]{h.getChangedAt(), h.getAction()});
            }
        } catch (IOException e) {
            histModel.addRow(new Object[]{"—", "Failed to load history"});
        }

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("History"));
        rightPanel.setPreferredSize(new Dimension(230, 0));
        rightPanel.add(new JScrollPane(histTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(320);
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);
        setVisible(true);
    }

    private void showUpdateStatusDialog(Task task) {
        String[] options = {
            Task.STATUS_TODO, Task.STATUS_IN_PROGRESS, Task.STATUS_DONE};
        String chosen = (String) JOptionPane.showInputDialog(
            this, "Select new status:", "Update Status",
            JOptionPane.PLAIN_MESSAGE, null, options, task.getStatus());
        if (chosen == null || chosen.equals(task.getStatus())) return;
        try {
            Message resp = clientSocket.updateStatus(
                task.getId(), chosen, currentUser.getId());
            if (MessageType.SUCCESS.equals(resp.getType())) {
                onUpdate.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, resp.getData(),
                    "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Network error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showReassignDialog(Task task, List<User> users) {
        String[] names = users.stream()
            .map(User::getUsername).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(
            this, "Select new assignee:", "Reassign Task",
            JOptionPane.PLAIN_MESSAGE, null, names, null);
        if (chosen == null) return;
        User target = users.stream()
            .filter(u -> u.getUsername().equals(chosen))
            .findFirst().orElse(null);
        if (target == null) return;
        try {
            Message resp = clientSocket.updateAssignee(
                task.getId(), target.getId(), currentUser.getId());
            if (MessageType.SUCCESS.equals(resp.getType())) {
                onUpdate.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, resp.getData(),
                    "Reassign Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Network error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<TaskHistory> parseHistories(String data, List<User> users) {
        List<TaskHistory> list = new java.util.ArrayList<>();
        if (data == null || data.isBlank()) return list;
        for (String part : data.split(";")) {
            if (part.isBlank()) continue;
            String[] f = part.split("~", -1);
            if (f.length < 5) continue;
            TaskHistory h = new TaskHistory();
            h.setId(Integer.parseInt(f[0]));
            h.setTaskId(Integer.parseInt(f[1]));
            h.setOperatorId(Integer.parseInt(f[2]));
            String opName = findUsername(users, Integer.parseInt(f[2]));
            h.setAction(opName + ": " + f[3]);
            h.setChangedAt(f[4]);
            list.add(h);
        }
        return list;
    }

    private String findUsername(List<User> users, int id) {
        return users.stream()
            .filter(u -> u.getId() == id)
            .map(User::getUsername)
            .findFirst().orElse("User " + id);
    }
}