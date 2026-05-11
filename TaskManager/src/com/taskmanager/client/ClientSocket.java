package com.taskmanager.client;

import com.taskmanager.common.Message;
import com.taskmanager.common.MessageType;

import java.io.*;
import java.net.Socket;

public class ClientSocket {

    private static final String HOST = "localhost";
    private static final int    PORT = 8888;

    private Socket       socket;
    private BufferedReader in;
    private PrintWriter    out;

    // 连接服务器，失败抛异常
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        in  = new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(
                  new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        System.out.println("已连接到服务器");
    }

    // 发送请求并等待服务器响应（同步）
    public Message sendAndReceive(Message request) throws IOException {
        out.println(request.serialize());          // 发送
        String raw = in.readLine();                // 等待响应（阻塞）
        if (raw == null) throw new IOException("服务器已断开");
        return Message.deserialize(raw);
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ── 封装各种请求，让上层代码更简洁 ──

    public Message login(String username, String password) throws IOException {
        return sendAndReceive(new Message(MessageType.LOGIN,
                                          username + "|||" + password));
    }

    public Message register(String username, String password) throws IOException {
        return sendAndReceive(new Message(MessageType.REGISTER,
                                          username + "|||" + password));
    }

    public Message getAllTasks() throws IOException {
        return sendAndReceive(new Message(MessageType.GET_ALL_TASKS, ""));
    }

    public Message getMyTasks(int userId) throws IOException {
        return sendAndReceive(new Message(MessageType.GET_MY_TASKS,
                                          String.valueOf(userId)));
    }

    public Message createTask(String title, String desc, String priority,
                               int creatorId, int assigneeId) throws IOException {
        String data = title + "|||" + desc + "|||" + priority
                    + "|||" + creatorId + "|||" + assigneeId;
        return sendAndReceive(new Message(MessageType.CREATE_TASK, data));
    }

    public Message updateStatus(int taskId, String newStatus,
                                 int operatorId) throws IOException {
        String data = taskId + "|||" + newStatus + "|||" + operatorId;
        return sendAndReceive(new Message(MessageType.UPDATE_STATUS, data));
    }

    public Message updateAssignee(int taskId, int assigneeId,
                                   int operatorId) throws IOException {
        String data = taskId + "|||" + assigneeId + "|||" + operatorId;
        return sendAndReceive(new Message(MessageType.UPDATE_ASSIGNEE, data));
    }

    public Message getHistory(int taskId) throws IOException {
        return sendAndReceive(new Message(MessageType.GET_HISTORY,
                                          String.valueOf(taskId)));
    }

    public Message getAllUsers() throws IOException {
        return sendAndReceive(new Message(MessageType.GET_ALL_USERS, ""));
    }
}