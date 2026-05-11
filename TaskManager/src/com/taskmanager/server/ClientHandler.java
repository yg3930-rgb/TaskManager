package com.taskmanager.server;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Set<ClientHandler> onlineClients;
    private final TaskService taskService = new TaskService();

    private BufferedReader in;
    private PrintWriter    out;
    private String         currentUsername;

    public ClientHandler(Socket socket, Set<ClientHandler> onlineClients) {
        this.socket = socket;
        this.onlineClients = onlineClients;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(
                      new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(
                      new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = in.readLine()) != null) {
                Message request = Message.deserialize(line);
                System.out.println("[Received] " + currentUsername + " → " + request);

                Message response = handleRequest(request);
                sendMessage(response);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + currentUsername);
        } finally {
            onlineClients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Message handleRequest(Message request) {
        return switch (request.getType()) {
            case MessageType.LOGIN           -> taskService.login(request.getData());
            case MessageType.REGISTER        -> taskService.register(request.getData());
            case MessageType.GET_ALL_TASKS   -> taskService.getAllTasks();
            case MessageType.GET_MY_TASKS    -> taskService.getMyTasks(request.getData());
            case MessageType.CREATE_TASK     -> taskService.createTask(request.getData());
            case MessageType.UPDATE_STATUS   -> taskService.updateStatus(request.getData());
            case MessageType.UPDATE_ASSIGNEE -> taskService.updateAssignee(request.getData());
            case MessageType.GET_HISTORY     -> taskService.getHistory(request.getData());
            case MessageType.GET_ALL_USERS   -> taskService.getAllUsers();
            case MessageType.LOGOUT          -> { currentUsername = null;
                                                  yield new Message(MessageType.SUCCESS, "Logged out"); }
            default -> new Message(MessageType.ERROR, "Unknown message type: " + request.getType());
        };
    }

    public void sendMessage(Message msg) {
        out.println(msg.serialize());
    }

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String name) { this.currentUsername = name; }
}