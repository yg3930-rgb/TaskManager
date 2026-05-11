package com.taskmanager.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:taskmanager.db";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connected successfully");
            initTables();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Singleton: only one connection instance for the entire program
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // Create three tables if they do not already exist
    private void initTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Users table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            )
        """);

        // Tasks table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                title       TEXT    NOT NULL,
                description TEXT,
                status      TEXT    NOT NULL DEFAULT 'TODO',
                priority    TEXT    NOT NULL DEFAULT 'MEDIUM',
                creator_id  INTEGER NOT NULL,
                assignee_id INTEGER DEFAULT 0,
                created_at  TEXT    DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (creator_id)  REFERENCES users(id),
                FOREIGN KEY (assignee_id) REFERENCES users(id)
            )
        """);

        // Task history table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS task_history (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id     INTEGER NOT NULL,
                operator_id INTEGER NOT NULL,
                action      TEXT    NOT NULL,
                changed_at  TEXT    DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (task_id)     REFERENCES tasks(id),
                FOREIGN KEY (operator_id) REFERENCES users(id)
            )
        """);

        System.out.println("Tables initialized successfully");
        stmt.close();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}