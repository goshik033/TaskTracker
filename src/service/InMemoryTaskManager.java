package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InMemoryTaskManager implements TaskManager {
    private final HashMap<Integer, Task> taskHashMap = new HashMap<>();
    private final HashMap<Integer, Epic> epicHashMap = new HashMap<>();
    private final HashMap<Integer, SubTask> subTaskHashMap = new HashMap<>();
    private int id = 0;

    public int nextId() {
        return id++;
    }

    @Override
    public int addTask(Task task) {
        task.setId(nextId());
        taskHashMap.put(task.getId(), task);
        return task.getId();
    }

    @Override
    public int addEpic(Epic epic) {
        epic.setId(nextId());
        epic.setSubTaskIds(new ArrayList<>());
        epicHashMap.put(epic.getId(), epic);
        return epic.getId();
    }

    @Override
    public int addSubtask(SubTask subTask) {
        Epic epic = epicHashMap.get(subTask.getEpicId());
        if (epic == null) throw new IllegalArgumentException("model.Epic not found: " + subTask.getEpicId());
        subTask.setId(nextId());
        subTaskHashMap.put(subTask.getId(), subTask);
        epic.getSubTaskIds().add(subTask.getId());
        recalcEpicStatus(epic.getId());
        return subTask.getId();
    }

    public void recalcEpicStatus(int epicId) {
        Epic e = epicHashMap.get(epicId);
        if (e == null) return;
        List<Integer> ids = e.getSubTaskIds();
        if (ids.isEmpty()) {
            e.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true, allDone = true;
        for (int sid : ids) {
            Status st = subTaskHashMap.get(sid).getStatus();
            if (st != Status.NEW) allNew = false;
            if (st != Status.DONE) allDone = false;
        }
        if (allDone) e.setStatus(Status.DONE);
        else if (allNew) e.setStatus(Status.NEW);
        else e.setStatus(Status.IN_PROGRESS);
    }


    public Task getTask(int id) {
        return taskHashMap.get(id);
    }


    public Epic getEpic(int id) {

        return epicHashMap.get(id);
    }


    public SubTask getSubTask(int id) {
        return subTaskHashMap.get(id);
    }

    @Override
    public void deleteTask(int id) {
        taskHashMap.remove(id);
    }

    @Override
    public void deleteEpic(int id) {
        Epic e = epicHashMap.remove(id);
        if (e == null) return;
        for (int sid : e.getSubTaskIds()) subTaskHashMap.remove(sid);
    }

    @Override
    public void deleteSubtask(int id) {
        SubTask s = subTaskHashMap.remove(id);
        if (s == null) return;
        Epic e = epicHashMap.get(s.getEpicId());
        if (e != null) {
            e.getSubTaskIds().remove(Integer.valueOf(id));
            recalcEpicStatus(e.getId());
        }
    }

    @Override
    public boolean updateTask(int id, Task task) {
        if (!taskHashMap.containsKey(id)) return false;
        task.setId(id);
        taskHashMap.replace(id, task);
        return true;
    }

    @Override
    public boolean updateEpic(int id, Epic epic) {
        if (!epicHashMap.containsKey(id)) return false;
        Epic e = epicHashMap.get(id);
        e.setName(epic.getName());
        e.setDescription(epic.getDescription());
        recalcEpicStatus(epic.getId());
        return true;
    }

    @Override
    public boolean updateSubTask(int id, SubTask subTask) {
        if (!subTaskHashMap.containsKey(id)) return false;
        SubTask st = subTaskHashMap.get(id);
        st.setName(subTask.getName());
        st.setDescription(subTask.getDescription());
        recalcEpicStatus(subTask.getEpicId());
        return true;
    }


    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(taskHashMap.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epicHashMap.values());
    }

    @Override
    public List<SubTask> getAllSubTasks() {
        return new ArrayList<>(subTaskHashMap.values());
    }

    @Override
    public void deleteAllTasks() {
        taskHashMap.clear();
    }

    @Override
    public void deleteAllEpics() {
        epicHashMap.clear();
        subTaskHashMap.clear();
    }

    @Override
    public void deleteAllSubTasks() {
        subTaskHashMap.clear();
        for (Epic e : epicHashMap.values()) {
            e.getSubTaskIds().clear();
            e.setStatus(Status.NEW);
        }
    }

    @Override
    public List<SubTask> getEpicsSubTasks(int id) {
        Epic e = epicHashMap.get(id);
        if (e == null) return List.of();
        List<Integer> list = e.getSubTaskIds();
        List<SubTask> subTasks = new ArrayList<>();
        for (int sid : list) {
            SubTask s = subTaskHashMap.get(sid);
            if (s != null) subTasks.add(s);
        }
        return subTasks;
    }

    @Override
    public void setTaskStatus(int id, Status status) {
        Task t = taskHashMap.get(id);
        if (t == null) throw new IllegalArgumentException("model.Task не найден: " + id);
        t.setStatus(status);
    }

    @Override
    public void setSubTaskStatus(int id, Status status) {
        SubTask st = subTaskHashMap.get(id);
        if (st == null) throw new IllegalArgumentException("model.SubTask не найден: " + id);
        st.setStatus(status);
        recalcEpicStatus(st.getEpicId());
    }


}


