import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Manager {
    private final HashMap<Integer, Task> taskHashMap = new HashMap<>();
    private final HashMap<Integer, Epic> epicHashMap = new HashMap<>();
    private final HashMap<Integer, SubTask> subTaskHashMap = new HashMap<>();
    private int id = 0;

    public int nextId() {
        return id++;
    }

    public int addTask(Task task) {
        task.setId(nextId());
        taskHashMap.put(task.getId(), task);
        return task.getId();
    }

    public int addEpic(Epic epic) {
        epic.setId(nextId());
        epic.setSubtaskIds(new ArrayList<>());
        epicHashMap.put(epic.getId(), epic);
        return epic.getId();
    }

    public int addSubtask(SubTask subTask) {
        Epic epic = epicHashMap.get(subTask.getEpicId());
        if (epic == null) throw new IllegalArgumentException("Epic not found: " + subTask.getEpicId());
        subTask.setId(nextId());
        subTaskHashMap.put(subTask.getId(), subTask);
        epic.getSubtaskIds().add(subTask.getId());
        recalcEpicStatus(epic.getId());
        return subTask.getId();
    }

    private void recalcEpicStatus(int epicId) {
        Epic e = epicHashMap.get(epicId);
        if (e == null) return;
        List<Integer> ids = e.getSubtaskIds();
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

    public SubTask getSubtask(int id) {

        return subTaskHashMap.get(id);
    }

    public void deleteTask(int id) {
        taskHashMap.remove(id);
    }

    public void deleteEpic(int id) {
        Epic e = epicHashMap.remove(id);
        if (e == null) return;
        for (int sid : e.getSubtaskIds()) subTaskHashMap.remove(sid);
    }

    public void deleteSubtask(int id) {
        SubTask s = subTaskHashMap.remove(id);
        if (s == null) return;
        Epic e = epicHashMap.get(s.getEpicId());
        if (e != null) {
            e.getSubtaskIds().remove(Integer.valueOf(id));
            recalcEpicStatus(e.getId());
        }
    }

    public boolean updateTask(int id, Task task) {
        if (!taskHashMap.containsKey(id)) return false;
        task.setId(id);
        taskHashMap.replace(id, task);
        return true;
    }

    public boolean updateEpic(int id, Epic epic) {
        if (!epicHashMap.containsKey(id)) return false;
        Epic e = epicHashMap.get(id);
        e.setName(epic.getName());
        e.setDescription(epic.getDescription());
        recalcEpicStatus(epic.getId());
        return true;
    }

    public boolean updateSubTask(int id, SubTask subTask) {
        if (!subTaskHashMap.containsKey(id)) return false;
        subTask.setId(id);
        recalcEpicStatus(subTask.getEpicId());
        subTaskHashMap.replace(id, subTask);
        return true;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(taskHashMap.values());
    }

    public List<Epic> getAllEpics() {
        return new ArrayList<>(epicHashMap.values());
    }

    public List<SubTask> getAllSubTasks() {
        return new ArrayList<>(subTaskHashMap.values());
    }

    public void deleteAllTasks() {
        taskHashMap.clear();
    }

    public void deleteAllEpics() {
        epicHashMap.clear();
        subTaskHashMap.clear();
    }

    public void deleteAllSubTasks() {
        subTaskHashMap.clear();
        for (Epic e : epicHashMap.values()) {
            e.getSubtaskIds().clear();
            e.setStatus(Status.NEW);
        }
    }

    public List<SubTask> getEpicsSubTasks(int id) {
        Epic e = epicHashMap.get(id);
        if (e == null) return List.of();
        List<Integer> list = e.getSubtaskIds();
        List<SubTask> subTasks = new ArrayList<>();
        for (int sid : list) {
            SubTask s = subTaskHashMap.get(sid);
            if (s != null) subTasks.add(s);
        }
        return subTasks;
    }


}
