package com.taskmanager.server;

import com.taskmanager.database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {

    private static final int PORT = 8888;

    // 存所有在线的 ClientHandler，用于后续广播（线程安全的 Set）
    private static final Set<ClientHandler> onlineClients =
        Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        // 启动时先初始化数据库
        DatabaseManager.getInstance();
        System.out.println("Server started, listening on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // 阻塞，等待新连接
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, onlineClients);
                onlineClients.add(handler);

                // 每个客户端独立一个线程
                Thread thread = new Thread(handler);
                thread.setDaemon(true);   // 守护线程，主线程退出时自动结束
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server start fail: " + e.getMessage());
        }
    }

    public static Set<ClientHandler> getOnlineClients() {
        return onlineClients;
    }
}