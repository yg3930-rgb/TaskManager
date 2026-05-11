package com.taskmanager.client;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateTaskDialog extends JDialog {

    private final User currentUser;
    private final ClientSocket clientSocket;
    private final Runnable onSuccess;

    private JTextField titleField;
    private JTextArea  descArea;
    private JComboBox<String> priorityBox;
    private JComboBox<String> assigneeBox;
    private List<User> allUsers = new ArrayList<>();

    public CreateTaskDialog(JFrame parent, User currentUser,
                            ClientSocket clientSocket, Runnable onSuccess) {
        super(parent, "New Task", true);
        this.currentUser  = currentUser;
        this.clientSocket = clientSocket;
        this.onSuccess    = onSuccess;
        initUI();
    }

    private void initUI() {
        setSize(380, 340);
        setLocationRelativeTo(getParent());
        setResizable(false);

        try {
            Message resp = clientSocket.getAllUsers();
            allUsers = MainFrame.parseUsers(resp.getData());
        } catch (IOException e) {
            allUsers = new ArrayList<>();
        }

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        titleField = new JTextField(20);
        descArea   = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);

        priorityBox = new JComboBox<>(new String[]{
            Task.PRIORITY_LOW, Task.PRIORITY_MEDIUM, Task.PRIORITY_HIGH});
        priorityBox.setSelectedItem(Task.PRIORITY_MEDIUM);

        String[] usernames = allUsers.stream()
            .map(User::getUsername).toArray(String[]::new);
        assigneeBox = new JComboBox<>(usernames);

        int r = 0;
        addRow(panel, gbc, r++, "Title *",   titleField);
        addRow(panel, gbc, r++, "Description", descScroll);
        addRow(panel, gbc, r++, "Priority",  priorityBox);
        addRow(panel, gbc, r++, "Assign To", assigneeBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelBtn = new JButton("Cancel");
        JButton createBtn = new JButton("Create");
        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> doCreate());
        btnPanel.add(cancelBtn);
        btnPanel.add(createBtn);

        setLayout(new BorderLayout());
        add(panel,    BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void doCreate() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.",
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int assigneeId = (assigneeBox.getSelectedIndex() >= 0 && !allUsers.isEmpty())
            ? allUsers.get(assigneeBox.getSelectedIndex()).getId() : 0;

        try {
            Message resp = clientSocket.createTask(
                title,
                descArea.getText().trim(),
                (String) priorityBox.getSelectedItem(),
                currentUser.getId(),
                assigneeId
            );
            if (MessageType.SUCCESS.equals(resp.getType())) {
                onSuccess.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, resp.getData(),
                    "Create Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Network error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addRow(JPanel p, GridBagConstraints gbc,
                        int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(field, gbc);
    }
}