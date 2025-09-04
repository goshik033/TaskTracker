package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.util.List;

public interface TaskManager {

    int addTask(Task task);

    int addEpic(Epic epic);

    int addSubtask(SubTask subTask);

    SubTask getSubTask(int id);

    Epic getEpic(int id);

    Task getTask(int id);

    void deleteTask(int id);

    void deleteEpic(int id);

    void deleteSubtask(int id);

    boolean updateTask(int id, Task task);

    boolean updateEpic(int id, Epic epic);

    boolean updateSubTask(int id, SubTask subTask);

    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<SubTask> getAllSubTasks();

    void deleteAllTasks();

    void deleteAllEpics();

    void deleteAllSubTasks();

    List<SubTask> getEpicsSubTasks(int id);

    void setTaskStatus(int id, Status status);

    void setSubTaskStatus(int id, Status status);

    List<Task> getHistory();


}
