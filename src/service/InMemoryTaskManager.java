package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> taskHashMap = new LinkedHashMap<>();
    private final Map<Integer, Epic> epicHashMap = new LinkedHashMap<>();
    private final Map<Integer, SubTask> subTaskHashMap = new LinkedHashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private final TimeGridArray timeGridArray = new TimeGridArray(LocalDateTime.now());
    private final Set<Task> prioritized = new TreeSet<>(new Comparator<Task>() {
        @Override
        public int compare(Task t1, Task t2) {
            if (t1.getStartTime() == null && t2.getStartTime() == null) {
                return Integer.compare(t1.getId(), t2.getId());
            }


            if (t1.getStartTime() == null) {
                return 1;
            }
            if (t2.getStartTime() == null) {
                return -1;
            }

            int cmp = t1.getStartTime().compareTo(t2.getStartTime());
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(t1.getId(), t2.getId());
        }
    });

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritized);
    }


    private int id = 0;

    public int nextId() {
        return id++;
    }

    public void setCurrentId(int oldId) {
        id = oldId + 1;
    }


    protected void putTask(Task t) {
        taskHashMap.put(t.getId(), t);
    }

    protected void putEpic(Epic e) {
        if (e.getSubTaskIds() == null) e.setSubTaskIds(new ArrayList<>());
        epicHashMap.put(e.getId(), e);
    }

    protected void putSubTask(SubTask s) {
        Epic e = epicHashMap.get(s.getEpicId());
        if (e == null) throw new IllegalStateException("Epic " + s.getEpicId() + " not loaded yet");
        subTaskHashMap.put(s.getId(), s);
        e.getSubTaskIds().add(s.getId());
    }

    @Override
    public OptionalInt addTask(Task task) {
        if (task == null) return OptionalInt.empty();
        return saveAuto(task);
    }

    @Override
    public OptionalInt addEpic(Epic epic) {
        if (epic == null) return OptionalInt.empty();
        if (epic.getSubTaskIds() == null) {
            epic.setSubTaskIds(new ArrayList<>());
        }
        return saveAuto(epic);
    }

    @Override
    public OptionalInt addSubTask(SubTask subTask) {
        if (subTask == null) return OptionalInt.empty();

        Epic epic = epicHashMap.get(subTask.getEpicId());
        if (epic == null) {
            return OptionalInt.empty();
        }

        prioritized.remove(epic);

        OptionalInt oi = saveAuto(subTask);
        if (oi.isPresent()) {
            int id = oi.getAsInt();
            epic.getSubTaskIds().add(id);

            recalcEpicStatus(epic.getId());
            recalcEpicTime(epic.getId());

            if (epic.getStartTime() != null) {
                prioritized.add(epic);
            }
        }

        return oi;
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

    public void recalcEpicTime(int epicId) {
        Epic e = epicHashMap.get(epicId);

        List<Integer> ids = e.getSubTaskIds();
        if (ids == null || ids.isEmpty()) {
            e.setStartTime(null);
            e.setDuration(null);
            return;
        }
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        Duration sum = Duration.ZERO;

        for (Integer i : ids) {
            SubTask s = subTaskHashMap.get(i);
            if (s.getStartTime() == null || s.getDuration() == null) continue;
            LocalDateTime ss = s.getStartTime();
            LocalDateTime se = s.getEndTime();
            if (minStart == null || ss.isBefore(minStart)) minStart = ss;
            if (maxEnd == null || (se != null && se.isAfter(maxEnd))) maxEnd = se;
            sum = sum.plus(s.getDuration());
        }
        e.setStartTime(minStart);
        e.setDuration(sum);
    }


    public Task getTask(int id) {
        Task task = taskHashMap.get(id);
        if (task != null) historyManager.add(task);
        return task;
    }


    public Epic getEpic(int id) {
        Epic epic = epicHashMap.get(id);
        if (epic != null) historyManager.add(epic);
        return epic;
    }


    public SubTask getSubTask(int id) {
        SubTask subTask = subTaskHashMap.get(id);
        if (subTask != null) historyManager.add(subTask);
        return subTask;
    }

    @Override
    public void deleteTask(int id) {
        Task t = taskHashMap.remove(id);
        if (t == null) return;

        releaseIfReserved(t);
    }

    @Override
    public void deleteEpic(int id) {
        Epic e = epicHashMap.remove(id);
        if (e == null) return;


        List<Integer> subs = e.getSubTaskIds();
        if (subs != null) {
            for (int sid : subs) {
                SubTask s = subTaskHashMap.remove(sid);
                if (s == null) continue;
                releaseIfReserved(s);
            }
            subs.clear();
        }

    }


    @Override
    public void deleteSubtask(int id) {
        SubTask s = subTaskHashMap.remove(id);
        if (s == null) return;
        releaseIfReserved(s);
        Epic e = epicHashMap.get(s.getEpicId());
        if (e != null) {
            e.getSubTaskIds().remove(Integer.valueOf(id));
            recalcEpicStatus(e.getId());
            recalcEpicTime(e.getId());

        }
    }



    private void releaseIfReserved(Task t) {
        prioritized.remove(t);

        LocalDateTime s = t.getStartTime();
        Duration d = t.getDuration();
        if (s != null && d != null && !d.isZero()) {
            try { timeGridArray.release(s, d); } catch (IllegalArgumentException ignored) {}
        }
    }
    private void changeReservation(Task oldTask, Task newTask) {
        releaseIfReserved(oldTask);
        LocalDateTime s = newTask.getStartTime();
        Duration d = newTask.getDuration();

        if (s == null || d == null || d.isZero()) {
            oldTask.setStartTime(s);
            oldTask.setDuration(d);
            prioritized.add(oldTask);

        }
        try {
            if (timeGridArray.tryReserve(s, d)) {
                oldTask.setStartTime(s);
                oldTask.setDuration(d);
                prioritized.add(oldTask);
            }
        } catch (IllegalArgumentException ignored) {

        }

    }

    @Override
    public boolean updateTask(int id, Task task) {
        if (!taskHashMap.containsKey(id)) return false;
        Task t = taskHashMap.get(id);
        t.setName(task.getName());
        t.setDescription(task.getDescription());
        changeReservation(t,task);
        return true;
    }

    @Override
    public boolean updateEpic(int id, Epic epic) {
        if (!epicHashMap.containsKey(id)) return false;
        Epic e = epicHashMap.get(id);
        e.setName(epic.getName());
        e.setDescription(epic.getDescription());
        recalcEpicStatus(id);
        recalcEpicTime(id);

        return true;
    }

    @Override
    public boolean updateSubTask(int id, SubTask subTask) {
        if (!subTaskHashMap.containsKey(id)) return false;
        SubTask st = subTaskHashMap.get(id);
        st.setName(subTask.getName());
        st.setDescription(subTask.getDescription());
        recalcEpicStatus(subTask.getEpicId());
        recalcEpicTime(subTask.getEpicId());
        changeReservation(st,subTask);
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
        for (Task t : taskHashMap.values()) {
            releaseIfReserved(t);
        }
        taskHashMap.clear();
    }

    @Override
    public void deleteAllEpics() {
        epicHashMap.clear();
        for (SubTask s : subTaskHashMap.values()) {
            releaseIfReserved(s);
        }
        subTaskHashMap.clear();
    }

    @Override
    public void deleteAllSubTasks() {
        for (SubTask s : subTaskHashMap.values()) {
            releaseIfReserved(s);
        }
        subTaskHashMap.clear();
        for (Epic e : epicHashMap.values()) {
            e.getSubTaskIds().clear();
            e.setStatus(Status.NEW);
            recalcEpicTime(e.getId());
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
        if (t == null) throw new IllegalArgumentException("Task не найден: " + id);
        t.setStatus(status);
    }

    @Override
    public void setSubTaskStatus(int id, Status status) {
        SubTask st = subTaskHashMap.get(id);
        if (st == null) throw new IllegalArgumentException("SubTask не найден: " + id);
        st.setStatus(status);
        recalcEpicStatus(st.getEpicId());
        recalcEpicTime(st.getEpicId());
    }

    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    protected boolean hasTask(int id) {
        return taskHashMap.containsKey(id);
    }

    protected boolean hasSubTask(int id) {
        return subTaskHashMap.containsKey(id);
    }

    protected boolean hasEpic(int id) {
        return epicHashMap.containsKey(id);
    }


    private OptionalInt saveAuto(Task entity) {
        if (entity == null) return OptionalInt.empty();

        boolean isSub = entity instanceof SubTask;
        boolean isEpic = entity instanceof Epic;

        java.time.LocalDateTime start = isEpic ? null : entity.getStartTime();
        java.time.Duration dur = isEpic ? null : entity.getDuration();

        if (dur != null && dur.isNegative()) return OptionalInt.empty();

        if (start == null || dur == null || dur.isZero()) {
            int id = nextId();
            if (isSub) {
                SubTask st = (SubTask) entity;
                st.setId(id);
                subTaskHashMap.put(id, st);
                prioritized.add(st);
            } else if (isEpic) {
                Epic e = (Epic) entity;
                e.setId(id);
                epicHashMap.put(id, e);

            } else {
                entity.setId(id);
                taskHashMap.put(id, entity);
                prioritized.add(entity);
            }
            return OptionalInt.of(id);
        }

        try {
            if (timeGridArray.tryReserve(start, dur)) {
                int id = nextId();
                if (isSub) {
                    SubTask st = (SubTask) entity;
                    st.setId(id);
                    subTaskHashMap.put(id, st);
                    prioritized.add(st);
                } else {
                    entity.setId(id);
                    taskHashMap.put(id, entity);
                    prioritized.add(entity);
                }
                return OptionalInt.of(id);
            } else {
                return OptionalInt.empty();
            }
        } catch (IllegalArgumentException e) {
            return OptionalInt.empty();
        }
    }
}








