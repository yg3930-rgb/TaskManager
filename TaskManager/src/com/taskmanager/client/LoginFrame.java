package com.taskmanager.client;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;
import com.taskmanager.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private ClientSocket clientSocket;

    public LoginFrame() {
        clientSocket = new ClientSocket();
        try {
            clientSocket.connect();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to server. Please start the server first.",
                "Connection Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        initUI();
    }

    private void initUI() {
        setTitle("Task Manager — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320, 280);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = new JTextField(16);
        passwordField = new JPasswordField(16);

        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        loginBtn.addActionListener(e -> doLogin());

        JButton registerBtn = new JButton("No account? Register");
        registerBtn.setBorderPainted(false);
        registerBtn.setContentAreaFilled(false);
        registerBtn.setForeground(Color.BLUE.darker());
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.addActionListener(e -> showRegisterDialog());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Username"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Password"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(passwordField, gbc);

        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(loginBtn);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(registerBtn);

        add(mainPanel);
        setVisible(true);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Username and password cannot be empty.",
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Message resp = clientSocket.login(username, password);
            if (MessageType.LOGIN_SUCCESS.equals(resp.getType())) {
                String[] parts = resp.getData().split("\\|\\|\\|");
                User user = new User(Integer.parseInt(parts[0]), parts[1], "");
                dispose();
                new MainFrame(user, clientSocket);
            } else {
                JOptionPane.showMessageDialog(this, resp.getData(),
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Network error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRegisterDialog() {
        String username = JOptionPane.showInputDialog(this, "Enter username:");
        if (username == null || username.trim().isEmpty()) return;
        String password = JOptionPane.showInputDialog(this, "Enter password:");
        if (password == null || password.trim().isEmpty()) return;
        try {
            Message resp = clientSocket.register(username.trim(), password.trim());
            if (MessageType.SUCCESS.equals(resp.getType())) {
                JOptionPane.showMessageDialog(this,
                    "Registration successful! Please log in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, resp.getData(),
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Network error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}