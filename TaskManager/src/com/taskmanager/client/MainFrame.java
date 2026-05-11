package com.taskmanager.client;

import com.taskmanager.common.Message;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {

    private final User currentUser;
    private final ClientSocket clientSocket;

    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel refreshIndicator;

    private Map<Integer, String> userCache = new HashMap<>();

    private ScheduledExecutorService scheduler;
    private volatile boolean isAutoRefresh = true;
    private String lastTaskSnapshot = "";

    private static final String[] COLUMNS =
        {"ID", "Title", "Status", "Priority", "Assignee", "Created At"};

    public MainFrame(User user, ClientSocket clientSocket) {
        this.currentUser  = user;
        this.clientSocket = clientSocket;
        initUI();
        loadTasks();
        startAutoRefresh();
    }

    private void initUI() {
        setTitle("Task Manager — " + currentUser.getUsername() + " logged in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 560);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopAutoRefresh();
            }
        });

        JPanel sidebar  = buildSidebar();
        JPanel toolbar  = buildToolbar();

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(28);
        taskTable.getTableHeader().setReorderingAllowed(false);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        taskTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        taskTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        taskTable.getColumnModel().getColumn(5).setPreferredWidth(140);

        taskTable.getColumnModel().getColumn(2).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int col) {
                    super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);
                    if (!isSelected) {
                        switch (String.valueOf(value)) {
                            case "TODO"        -> setForeground(new Color(180, 100, 0));
                            case "IN_PROGRESS" -> setForeground(new Color(0, 100, 180));
                            case "DONE"        -> setForeground(new Color(30, 130, 30));
                            default            -> setForeground(Color.BLACK);
                        }
                    } else {
                        setForeground(Color.WHITE);
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                    return this;
                }
            }
        );

        taskTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openTaskDetail();
            }
        });

        JScrollPane scrollPane = new JScrollPane(taskTable);

        statusLabel      = new JLabel("  " + currentUser.getUsername() + " · logged in");
        refreshIndicator = new JLabel("● Live sync  ");
        refreshIndicator.setForeground(new Color(30, 130, 30));
        refreshIndicator.setFont(refreshIndicator.getFont().deriveFont(11f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statusBar.add(statusLabel,      BorderLayout.WEST);
        statusBar.add(refreshIndicator, BorderLayout.EAST);

        JPanel contentArea = new JPanel(new BorderLayout());
        contentArea.add(toolbar,    BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(statusBar,  BorderLayout.SOUTH);

        add(sidebar,     BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        setVisible(true);
    }

    private void startAutoRefresh() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-refresh");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            if (!isAutoRefresh) return;
            try {
                Message userResp = clientSocket.getAllUsers();
                List<User> users = parseUsers(userResp.getData());
                userCache.clear();
                for (User u : users) userCache.put(u.getId(), u.getUsername());

                Message taskResp = clientSocket.getAllTasks();
                String newSnapshot = taskResp.getData();

                if (!newSnapshot.equals(lastTaskSnapshot)) {
                    lastTaskSnapshot = newSnapshot;
                    List<Task> tasks = parseTasks(newSnapshot);
                    SwingUtilities.invokeLater(() -> {
                        int selectedTaskId = getSelectedTaskId();
                        fillTable(tasks);
                        restoreSelection(selectedTaskId);
                        statusLabel.setText("  All tasks — " + tasks.size() + " total");
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    refreshIndicator.setText("● Disconnected  ");
                    refreshIndicator.setForeground(Color.RED);
                });
                stopAutoRefresh();
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private int getSelectedTaskId() {
        int row = taskTable.getSelectedRow();
        if (row < 0) return -1;
        return (int) tableModel.getValueAt(row, 0);
    }

    private void restoreSelection(int taskId) {
        if (taskId < 0) return;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((int) tableModel.getValueAt(i, 0) == taskId) {
                taskTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(130, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        sidebar.setBackground(new Color(248, 248, 248));

        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(makeSidebarLabel("VIEWS"));

        JButton btnAll  = makeSidebarBtn("All Tasks");
        JButton btnMine = makeSidebarBtn("My Tasks");
        btnAll.addActionListener(e -> { isAutoRefresh = true;  loadTasks();   });
        btnMine.addActionListener(e -> { isAutoRefresh = false; loadMyTasks(); });
        sidebar.add(btnAll);
        sidebar.add(btnMine);

        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(makeSidebarLabel("ACTIONS"));

        JButton btnNew    = makeSidebarBtn("+ New Task");
        JButton btnLogout = makeSidebarBtn("Log Out");
        btnNew.addActionListener(e -> showCreateTaskDialog());
        btnLogout.addActionListener(e -> {
            stopAutoRefresh();
            clientSocket.disconnect();
            dispose();
            new LoginFrame();
        });
        sidebar.add(btnNew);
        sidebar.add(btnLogout);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        JButton newBtn     = new JButton("+ New Task");
        JButton refreshBtn = new JButton("Refresh Now");
        newBtn.addActionListener(e -> showCreateTaskDialog());
        refreshBtn.addActionListener(e -> { isAutoRefresh = true; loadTasks(); });
        toolbar.add(newBtn);
        toolbar.add(refreshBtn);
        return toolbar;
    }

    void loadTasks() {
        new Thread(() -> {
            try {
                Message userResp = clientSocket.getAllUsers();
                List<User> users = parseUsers(userResp.getData());
                userCache.clear();
                for (User u : users) userCache.put(u.getId(), u.getUsername());

                Message resp = clientSocket.getAllTasks();
                lastTaskSnapshot = resp.getData();
                List<Task> tasks = parseTasks(lastTaskSnapshot);

                SwingUtilities.invokeLater(() -> {
                    fillTable(tasks);
                    statusLabel.setText("  All tasks — " + tasks.size() + " total");
                    refreshIndicator.setText("● Live sync  ");
                    refreshIndicator.setForeground(new Color(30, 130, 30));
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Failed to load tasks: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void loadMyTasks() {
        new Thread(() -> {
            try {
                Message userResp = clientSocket.getAllUsers();
                List<User> users = parseUsers(userResp.getData());
                userCache.clear();
                for (User u : users) userCache.put(u.getId(), u.getUsername());

                Message resp  = clientSocket.getMyTasks(currentUser.getId());
                List<Task> tasks = parseTasks(resp.getData());

                SwingUtilities.invokeLater(() -> {
                    fillTable(tasks);
                    statusLabel.setText("  My tasks — " + tasks.size() + " total");
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Failed to load tasks: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void fillTable(List<Task> tasks) {
        tableModel.setRowCount(0);
        for (Task t : tasks) {
            String assignee = t.getAssigneeId() == 0 ? "—"
                : userCache.getOrDefault(t.getAssigneeId(), "User " + t.getAssigneeId());
            tableModel.addRow(new Object[]{
                t.getId(), t.getTitle(), t.getStatus(),
                t.getPriority(), assignee, t.getCreatedAt()
            });
        }
    }

    private void openTaskDetail() {
        int row = taskTable.getSelectedRow();
        if (row < 0) return;
        int taskId = (int) tableModel.getValueAt(row, 0);
        new TaskDetailDialog(this, taskId, currentUser, clientSocket, () -> loadTasks());
    }

    private void showCreateTaskDialog() {
        new CreateTaskDialog(this, currentUser, clientSocket, () -> loadTasks());
    }

    static List<Task> parseTasks(String data) {
        List<Task> list = new ArrayList<>();
        if (data == null || data.isBlank()) return list;
        for (String part : data.split(";")) {
            if (part.isBlank()) continue;
            String[] f = part.split("~", -1);
            if (f.length < 8) continue;
            Task t = new Task();
            t.setId(Integer.parseInt(f[0]));
            t.setTitle(f[1]);
            t.setDescription(f[2]);
            t.setStatus(f[3]);
            t.setPriority(f[4]);
            t.setCreatorId(Integer.parseInt(f[5]));
            t.setAssigneeId(Integer.parseInt(f[6]));
            t.setCreatedAt(f[7]);
            list.add(t);
        }
        return list;
    }

    static List<User> parseUsers(String data) {
        List<User> list = new ArrayList<>();
        if (data == null || data.isBlank()) return list;
        for (String part : data.split(";")) {
            if (part.isBlank()) continue;
            String[] f = part.split("~", -1);
            if (f.length < 2) continue;
            list.add(new User(Integer.parseInt(f[0]), f[1], ""));
        }
        return list;
    }

    private JButton makeSidebarBtn(String text) {
        JButton btn = new JButton(text);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        return btn;
    }

    private JLabel makeSidebarLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(Color.GRAY);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 14, 2, 14));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}