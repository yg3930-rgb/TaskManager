# Collaborative Task Management System

## Author
Your Name — NetID

---

## Project Overview

A multi-user collaborative task management system built in Java. Multiple clients can connect to a shared server simultaneously to create, assign, update, and track tasks in real time. The system demonstrates concurrent access with synchronization to ensure data consistency.

---

## Advanced Topics Used

### 1. Java Socket Programming (Networking)
- A `ServerSocket` listens on port 8888 for incoming client connections.
- Each client communicates with the server over a persistent TCP socket connection (`ClientSocket.java`).
- Messages are serialized as plain-text strings and exchanged between client and server using `BufferedReader` / `PrintWriter` over the socket streams.
- **Files:** `Server.java`, `ClientSocket.java`, `ClientHandler.java`

### 2. Multithreading
- The server spawns a new `Thread` for every client that connects, handled by `ClientHandler`, allowing multiple users to interact with the system concurrently.
- On the client side, all network calls (`loadTasks`, `loadMyTasks`) run on background threads to prevent the Swing GUI from freezing.
- An auto-refresh mechanism uses `ScheduledExecutorService` to poll the server every 3 seconds and update the task list in real time across all connected clients.
- **Files:** `Server.java`, `ClientHandler.java`, `MainFrame.java`

### 3. Synchronization (Locks)
- `TaskService` uses a `ReentrantLock` (`writeLock`) to protect all write operations (create task, update status, reassign).
- This prevents race conditions when multiple clients attempt to modify shared task data simultaneously.
- **File:** `TaskService.java`

### 4. Swing-based GUI
- Full graphical interface built with Java Swing.
- Components used: `JFrame`, `JTable`, `JDialog`, `JTextField`, `JPasswordField`, `JTextArea`, `JComboBox`, `JScrollPane`, `JSplitPane`, custom `TableCellRenderer` for color-coded task status.
- **Files:** `LoginFrame.java`, `MainFrame.java`, `CreateTaskDialog.java`, `TaskDetailDialog.java`

### 5. Database Integration — JDBC with SQLite
- All data is persisted in a local SQLite database (`taskmanager.db`) using JDBC.
- Three tables: `users`, `tasks`, `task_history`.
- DAO pattern used for clean separation of database logic.
- **Files:** `DatabaseManager.java`, `UserDAO.java`, `TaskDAO.java`, `TaskHistoryDAO.java`

---

## Project Structure

```
TaskManager/
├── lib/
│   └── sqlite-jdbc-3.53.0.0.jar
├── src/
│   ├── com/taskmanager/model/
│   │   ├── User.java
│   │   ├── Task.java
│   │   └── TaskHistory.java
│   ├── com/taskmanager/common/
│   │   ├── Message.java
│   │   └── MessageType.java
│   ├── com/taskmanager/database/
│   │   ├── DatabaseManager.java
│   │   ├── UserDAO.java
│   │   ├── TaskDAO.java
│   │   └── TaskHistoryDAO.java
│   ├── com/taskmanager/server/
│   │   ├── Server.java
│   │   ├── ClientHandler.java
│   │   └── TaskService.java
│   └── com/taskmanager/client/
│       ├── ClientSocket.java
│       ├── LoginFrame.java
│       ├── MainFrame.java
│       ├── CreateTaskDialog.java
│       └── TaskDetailDialog.java
└── README.md
```

---

## Requirements

- Java 17 or higher (Java 23 recommended)
- No additional dependencies beyond the included `sqlite-jdbc` JAR

---

## How to Run

### Step 1 — Compile the project

From the project root directory:

```bash
mkdir -p out
javac -cp "lib/sqlite-jdbc-3.53.0.0.jar" \
      -d out \
      src/com/taskmanager/model/*.java \
      src/com/taskmanager/common/*.java \
      src/com/taskmanager/database/*.java \
      src/com/taskmanager/server/*.java \
      src/com/taskmanager/client/*.java
```

### Step 2 — Start the server

Open a terminal and run:

```bash
java -cp "out:lib/sqlite-jdbc-3.53.0.0.jar" com.taskmanager.server.Server
```

On Windows, replace `:` with `;`:

```bash
java -cp "out;lib/sqlite-jdbc-3.53.0.0.jar" com.taskmanager.server.Server
```

You should see:
```
Database connected successfully
Tables initialized
Server started, listening on port 8888...
```

Keep this terminal open.

### Step 3 — Register sample users (first run only)

In a second terminal, compile and run the setup helper:

```bash
java -cp "out:lib/sqlite-jdbc-3.53.0.0.jar" DBTest
```

This creates two users:
| Username | Password |
|----------|----------|
| alice    | 123456   |
| bob      | 123456   |

### Step 4 — Launch the client

In a new terminal:

```bash
java -cp "out:lib/sqlite-jdbc-3.53.0.0.jar" com.taskmanager.client.LoginFrame
```

Log in with `alice / 123456` or `bob / 123456`.

### Step 5 — Test multi-user (optional)

Open a second terminal and run the same client command again to launch a second client window. Log in as a different user. Changes made by one client will automatically appear on the other within 3 seconds.

---

## Running in Eclipse (Alternative)

1. Import the project: `File → Open Projects from File System`
2. Add the JAR: right-click project → `Build Path → Add External Archives` → select `lib/sqlite-jdbc-3.53.0.0.jar`
3. Run `Server.java` first (right-click → `Run As → Java Application`)
4. Then run `LoginFrame.java`

---

## Features

- **User registration and login** — secure per-session authentication
- **Create tasks** — title, description, priority (LOW / MEDIUM / HIGH), assignee
- **Assign tasks** — assign to any registered user at creation or reassign later
- **Update task status** — TODO → IN_PROGRESS → DONE, color-coded in the UI
- **Task history tracking** — every change (create, assign, status update) is logged with timestamp and operator
- **Real-time sync** — all connected clients refresh automatically every 3 seconds
- **Concurrent access** — multiple users can operate simultaneously with lock-based safety

---

## No API keys or external datasets required.

The database file (`taskmanager.db`) is created automatically on first run.

