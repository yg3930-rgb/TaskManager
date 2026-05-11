import com.taskmanager.database.*;
import com.taskmanager.model.*;

public class DBTest {
    public static void main(String[] args) {
        // Initialize database (tables created automatically)
        DatabaseManager.getInstance();

        UserDAO userDAO = new UserDAO();
        TaskDAO taskDAO = new TaskDAO();
        TaskHistoryDAO historyDAO = new TaskHistoryDAO();

        // Register two users
        userDAO.insertUser(new User(0, "alice", "123456"));
        userDAO.insertUser(new User(0, "bob",   "123456"));

        // Login test
        User alice = userDAO.login("alice", "123456");
        System.out.println("Login successful: " + alice.getUsername() + ", id=" + alice.getId());

        // Create a task
        Task task = new Task();
        task.setTitle("Complete database design");
        task.setDescription("Design and implement DAOs for all three tables");
        task.setCreatorId(alice.getId());
        task.setPriority(Task.PRIORITY_HIGH);
        int taskId = taskDAO.insertTask(task);
        System.out.println("Task created successfully, id=" + taskId);

        // Assign to bob
        User bob = userDAO.login("bob", "123456");
        taskDAO.updateAssignee(taskId, bob.getId());
        historyDAO.insert(new TaskHistory(taskId, alice.getId(), "Assigned to bob"));

        // Update status
        taskDAO.updateStatus(taskId, Task.STATUS_IN_PROGRESS);
        historyDAO.insert(new TaskHistory(taskId, bob.getId(), "Status changed to IN_PROGRESS"));

        // Query all tasks
        System.out.println("\n--- All Tasks ---");
        taskDAO.getAllTasks().forEach(System.out::println);

        // Query history
        System.out.println("\n--- Task History ---");
        historyDAO.getByTaskId(taskId).forEach(h ->
            System.out.println(h.getChangedAt() + " | " + h.getAction()));

        DatabaseManager.getInstance().close();
    }
}