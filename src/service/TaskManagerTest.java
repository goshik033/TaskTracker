package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

abstract class TaskManagerTest<T extends TaskManager> {
    protected T manager;

    protected abstract T createManager();

    @BeforeEach
    void setUp() {
        manager = createManager();
    }
    protected Task mkTask(String name) {
        Task t = new Task();
        t.setName(name);
        t.setDescription("desc");
        return t;
    }

    protected Epic mkEpic(String name) {
        Epic e = new Epic();
        e.setName(name);
        e.setDescription("desc");
        return e;
    }

    protected SubTask mkSub(String name, int epicId) {
        SubTask st = new SubTask();
        st.setName(name);
        st.setEpicId(epicId);
        st.setDescription("desc");
        return st;
    }
    private int addSub(Status status, int epicId) {
        SubTask st = new SubTask();
        st.setEpicId(epicId);
        OptionalInt sid = manager.addSubTask(st);
        assertTrue(sid.isPresent(), "Ожидался id подзадачи");
        int id = sid.getAsInt();
        manager.setSubTaskStatus(id, status);
        return id;
    }
//
//    @Test
//    void addAndGetTaskStandard() {
//        int id = manager.addTask(mkTask("T1"));
//        Task got = manager.getTask(id);
//        assertNotNull(got);
//        assertEquals("T1", got.getName());
//        assertTrue(manager.getAllTasks().stream().anyMatch(t -> t.getId() == id));
//    }
    @Test
    void getAllTasksEmpty() {
        List<Task> all = manager.getAllTasks();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }
    @Test
    void wrongIdBehaviourForTask() {
        assertNull(manager.getTask(999999));
        assertThrows(IllegalArgumentException.class, () -> manager.setTaskStatus(42, Status.DONE));
    }
    @Test
    void addSubTaskStandardEpicExists() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent(), "Эпик должен создаться");
        int epicId = epicOi.getAsInt();

        OptionalInt subOi = manager.addSubTask(mkSub("S1", epicId));
        assertTrue(subOi.isPresent(), "Подзадача должна создаться");
        int subId = subOi.getAsInt();

        SubTask s = manager.getSubTask(subId);
        assertNotNull(s);
        assertEquals(epicId, s.getEpicId());
        assertTrue(manager.getEpicsSubTasks(epicId).stream().anyMatch(x -> x.getId() == subId));
    }

    @Test
    void addSubTaskWrongEpicReturnsEmpty() {
        OptionalInt subOi = manager.addSubTask(mkSub("S1", 123456));
        assertTrue(subOi.isEmpty(), "Ожидался пустой результат для несуществующего эпика");
    }
    @Test
    void epicStatusEmptySubtasksisNEW() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());
    }


    @Test
    void EpicStatusAllSubtasksNEWisNEW() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        addSub(Status.NEW, epicId);
        addSub(Status.NEW, epicId);
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());
    }


    @Test
    void EpicStatusAllSubtasksDONEisDONE() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        addSub(Status.DONE, epicId);
        addSub(Status.DONE, epicId);
        assertEquals(Status.DONE, manager.getEpic(epicId).getStatus());
    }

    @Test
    void EpicStatusSubtasksMixedDONEandNEWisIN_PROGRESS() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        addSub(Status.NEW, epicId);
        addSub(Status.DONE, epicId);
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }

    @Test
    void EpicStatusSubtasksHasIN_PROGRESSisIN_PROGRESS() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        addSub(Status.NEW, epicId);
        addSub(Status.NEW, epicId);
        addSub(Status.IN_PROGRESS, epicId);
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }




    @Test
    void deleteAllSubTasksResetsEpicStatusToNEW() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        addSub(Status.DONE, epicId);
        manager.deleteAllSubTasks();

        assertTrue(manager.getEpicsSubTasks(epicId).isEmpty());
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());
    }


    @Test
    void deleteEpicRemovesItsSubtasks() {
        OptionalInt epicOi = manager.addEpic(mkEpic("E1"));
        assertTrue(epicOi.isPresent());
        int epicId = epicOi.getAsInt();

        OptionalInt subOi = manager.addSubTask(mkSub("S1", epicId));
        assertTrue(subOi.isPresent());
        int s1 = subOi.getAsInt();

        manager.deleteEpic(epicId);
        assertNull(manager.getEpic(epicId));
        assertNull(manager.getSubTask(s1));
    }


    private Integer tryAdd(Task t) {
        OptionalInt oi = manager.addTask(t);
        return oi.isPresent() ? oi.getAsInt() : null;
    }

    private Task mkTaskWithTime(String name, LocalDateTime start, Duration dur) {
        Task t = mkTask(name);
        t.setStartTime(start);
        t.setDuration(dur);
        return t;
    }



    @Test
    void addTaskWithoutTimeIsOk() {
        Task t = mkTask("T-no-time"); // start/duration = null
        Integer id = tryAdd(t);
        assertNotNull(id, "Ожидался id для задачи без времени");
        assertNotNull(manager.getTask(id));
    }

    @Test
    void addTaskWithTimeIsOk() {
        java.time.LocalDateTime start = java.time.LocalDate.now().atTime(10, 0);
        java.time.Duration dur = java.time.Duration.ofMinutes(30);

        Integer id = tryAdd(mkTaskWithTime("T-time", start, dur));
        assertNotNull(id, "Ожидался id для валидного времени");

        Task saved = manager.getTask(id);
        assertNotNull(saved);
        assertEquals(start, saved.getStartTime());
        assertEquals(dur, saved.getDuration());
    }

    @Test
    void addTaskOverlapDenied() {
        java.time.LocalDate day = java.time.LocalDate.now();

        Integer id1 = tryAdd(mkTaskWithTime("A", day.atTime(10, 0), java.time.Duration.ofMinutes(30)));
        assertNotNull(id1, "Первая задача должна создаться");

        Integer id2 = tryAdd(mkTaskWithTime("B", day.atTime(10, 15), java.time.Duration.ofMinutes(15)));
        assertNull(id2, "Вторая задача с пересечением не должна создаться");
    }

    @Test
    void addTaskZeroDurationAllowedEvenIfOverlaps() {
        java.time.LocalDate day = java.time.LocalDate.now();

        Integer id1 = tryAdd(mkTaskWithTime("A", day.atTime(10, 0), java.time.Duration.ofMinutes(30)));
        assertNotNull(id1);

        Integer id2 = tryAdd(mkTaskWithTime("B", day.atTime(10, 0), java.time.Duration.ZERO));
        assertNotNull(id2, "Задача с нулевой длительностью должна создаться даже при «пересечении»");
    }

    @Test
    void addTaskOutsideGridYearDenied() {
        int nextYear = java.time.LocalDate.now().getYear() + 1;
        java.time.LocalDateTime start = java.time.LocalDate.of(nextYear, 1, 1).atTime(0, 0);

        Integer id = tryAdd(mkTaskWithTime("T-next-year", start, java.time.Duration.ofMinutes(15)));
        assertNull(id, "Дата вне года сетки должна быть отклонена");
    }

    @Test
    void addTaskNegativeDurationRejected() {
        java.time.LocalDateTime start = java.time.LocalDate.now().atTime(12, 0);
        Task t = mkTaskWithTime("T-neg", start, java.time.Duration.ofMinutes(-5));

        int before = manager.getAllTasks().size();
        Integer id = tryAdd(t);

        assertNull(id, "Отрицательная длительность должна быть отклонена");
        assertEquals(before, manager.getAllTasks().size(), "Список задач не должен измениться");
    }


}