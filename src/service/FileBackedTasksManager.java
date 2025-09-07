package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileBackedTasksManager extends InMemoryTaskManager implements TaskManager {


    private final Path file;
    private boolean loading = false;


    public FileBackedTasksManager(Path file) {
        this.file = file;
    }

    public static FileBackedTasksManager loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            return new FileBackedTasksManager(path);
        }
        FileBackedTasksManager manager = new FileBackedTasksManager(path);
        manager.load();
        return manager;
    }

    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(serializeToCsv());
            writer.flush();

        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось сохранить данные в файле" + file, e);
        }
    }

    private void load() {

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // "id,type,name,status,description,epic"
            if (header == null) return;

            int maxId = -1;
            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                String[] split = line.split(",");
                int id = Integer.parseInt(split[0]);
                String name = split[2];
                Status status = Status.valueOf(split[3]);
                String description = split[4];
                switch (split[1]) {
                    case "SUBTASK" -> {
                        int epicId = Integer.parseInt(split[5]);
                        SubTask subTask = new SubTask(id, name, description, status, epicId);
                        putSubTask(subTask);
                    }
                    case "EPIC" -> {

                        Epic epic = new Epic(id, name, description, status);
                        putEpic(epic);
                    }
                    case "TASK" -> {
                        Task task = new Task(id, name, description, status);
                        putTask(task);
                    }
                }
                maxId = Math.max(maxId, id);


            }
            setCurrentId(maxId);
            String historyLine = reader.readLine();
            if (historyLine != null && !historyLine.isBlank()) {
                for (String idStr : historyLine.split(",")) {
                    int id = Integer.parseInt(idStr.trim());
                    if (hasTask(id))
                        getTask(id);
                    else if (hasEpic(id))
                        getEpic(id);
                    else if (hasSubTask(id))
                        getSubTask(id);

                }
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось прочитать данные в файле" + file, e);
        }
    }


    private String serializeToCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,type,name,status,description,epic").append("\n");

        for (Task t : getAllTasks()) {
            sb.append(taskToCsv(t)).append("\n");
        }
        for (Epic e : getAllEpics()) {
            sb.append(taskToCsv(e)).append("\n");
        }
        for (SubTask s : getAllSubTasks()) {
            sb.append(taskToCsv(s)).append("\n");
        }

        sb.append("\n");
        sb.append(historyToCsv(super.getHistory()));
        return sb.toString();
    }

    private String taskToCsv(Task task) {
        String name = task.getName();
        String desc = task.getDescription();
        if (task instanceof SubTask s) {
            return String.format("%d,SUBTASK,%s,%s,%s,%d",
                    task.getId(), name, task.getStatus(), desc, s.getEpicId());
        } else if (task instanceof Epic e) {
            return String.format("%d,EPIC,%s,%s,%s,",
                    task.getId(), name, task.getStatus(), desc);

        } else {
            System.out.println(task.getStatus() + " " + task.getId());
            return String.format("%d,TASK,%s,%s,%s,",
                    task.getId(), name, task.getStatus(), desc);
        }
    }

    private String historyToCsv(List<Task> list) {

        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).getId());
        }
        return sb.toString();
    }

    @Override
    public List<Task> getHistory() {
        return super.getHistory();
    }

    @Override
    public int nextId() {
        return super.nextId();
    }

    @Override
    public int addTask(Task task) {
        int id = super.addTask(task);
        save();
        return id;

    }


    @Override
    public int addEpic(Epic epic) {
        int id = super.addEpic(epic);
        save();
        return id;
    }

    @Override
    public int addSubTask(SubTask subTask) {
        int id = super.addSubTask(subTask);
        save();
        return id;
    }


    @Override
    public Task getTask(int id) {
        Task task = super.getTask(id);
        save();
        return task;
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = super.getEpic(id);
        save();

        return epic;
    }

    @Override
    public SubTask getSubTask(int id) {
        SubTask subTask = super.getSubTask(id);
        save();
        return subTask;
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save();
    }

    @Override
    public boolean updateTask(int id, Task task) {
        boolean result = super.updateTask(id, task);
        save();
        return result;
    }

    @Override
    public boolean updateEpic(int id, Epic epic) {
        boolean result = super.updateEpic(id, epic);
        save();
        return result;
    }

    @Override
    public boolean updateSubTask(int id, SubTask subTask) {
        boolean result = super.updateSubTask(id, subTask);
        save();
        return result;
    }

    @Override
    public List<Task> getAllTasks() {
        return super.getAllTasks();
    }

    @Override
    public List<Epic> getAllEpics() {
        return super.getAllEpics();
    }

    @Override
    public List<SubTask> getAllSubTasks() {
        return super.getAllSubTasks();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void deleteAllSubTasks() {
        super.deleteAllSubTasks();
        save();
    }

    @Override
    public List<SubTask> getEpicsSubTasks(int id) {
        return super.getEpicsSubTasks(id);
    }

    @Override
    public void setTaskStatus(int id, Status status) {

        super.setTaskStatus(id, status);
        save();
    }

    @Override
    public void setSubTaskStatus(int id, Status status) {
        super.setSubTaskStatus(id, status);
        save();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }


    @Override
    public String toString() {
        return super.toString();
    }


}
