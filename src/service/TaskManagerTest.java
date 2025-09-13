package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
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
        LocalDateTime start = LocalDate.now().atTime(10, 0);
        Duration dur = Duration.ofMinutes(30);

        Integer id = tryAdd(mkTaskWithTime("T-time", start, dur));
        assertNotNull(id, "Ожидался id для валидного времени");

        Task saved = manager.getTask(id);
        assertNotNull(saved);
        assertEquals(start, saved.getStartTime());
        assertEquals(dur, saved.getDuration());
    }

    @Test
    void addTaskOverlapDenied() {
        LocalDate day = LocalDate.now();

        Integer id1 = tryAdd(mkTaskWithTime("A", day.atTime(10, 0), Duration.ofMinutes(30)));
        assertNotNull(id1, "Первая задача должна создаться");

        Integer id2 = tryAdd(mkTaskWithTime("B", day.atTime(10, 15), Duration.ofMinutes(15)));
        assertNull(id2, "Вторая задача с пересечением не должна создаться");
    }

    @Test
    void addTaskZeroDurationAllowedEvenIfOverlaps() {
        LocalDate day = LocalDate.now();

        Integer id1 = tryAdd(mkTaskWithTime("A", day.atTime(10, 0), Duration.ofMinutes(30)));
        assertNotNull(id1);

        Integer id2 = tryAdd(mkTaskWithTime("B", day.atTime(10, 0), Duration.ZERO));
        assertNotNull(id2, "Задача с нулевой длительностью должна создаться даже при «пересечении»");
    }

    @Test
    void addTaskOutsideGridYearDenied() {
        int nextYear = LocalDate.now().getYear() + 1;
        LocalDateTime start = LocalDate.of(nextYear, 1, 1).atTime(0, 0);

        Integer id = tryAdd(mkTaskWithTime("T-next-year", start, Duration.ofMinutes(15)));
        assertNull(id, "Дата вне года сетки должна быть отклонена");
    }

    @Test
    void addTaskNegativeDurationRejected() {
        LocalDateTime start = LocalDate.now().atTime(12, 0);
        Task t = mkTaskWithTime("T-neg", start, Duration.ofMinutes(-5));

        int before = manager.getAllTasks().size();
        Integer id = tryAdd(t);

        assertNull(id, "Отрицательная длительность должна быть отклонена");
        assertEquals(before, manager.getAllTasks().size(), "Список задач не должен измениться");
    }



    @Test
    void prioritizedOrdersByStartTimeThenId() {
        LocalDateTime base = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
        int idB = mustAddTask(mkTaskAt("B", base.plusHours(1), Duration.ofMinutes(10))); // 10:00
        int idA = mustAddTask(mkTaskAt("A", base,           Duration.ofMinutes(10)));     // 09:00
        int idC = mustAddTask(mkTaskAt("C", base.plusHours(2), Duration.ofMinutes(10)));  // 11:00

        List<Task> p = manager.getPrioritizedTasks();
        assertEquals(List.of(idA, idB, idC),
                p.stream().map(Task::getId).toList(),
                "Должно сортировать по startTime возр., при равенстве — по id");
    }

    @Test
    void prioritizedNullStartGoesLast() {
        LocalDateTime t10 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        int withTime = mustAddTask(mkTaskAt("T", t10, Duration.ofMinutes(15)));
        int noTime   = mustAddTask(mkTask("NT")); // start/dur = null → кладётся в конец

        List<Task> p = manager.getPrioritizedTasks();
        assertEquals(withTime, p.get(0).getId());
        assertEquals(noTime,   p.get(p.size() - 1).getId(),
                "Записи без времени должны идти в конце");
    }


    @Test
    void prioritizedIncludesSubtasks() {
        int epicId = mustAddEpic(mkEpic("E"));
        LocalDateTime t9  = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime t10 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        int tid  = mustAddTask(mkTaskAt("T", t10, Duration.ofMinutes(30)));
        int sid  = mustAddSub(mkSubAt("S", epicId, t9, Duration.ofMinutes(20)));

        List<Task> p = manager.getPrioritizedTasks();
        assertTrue(p.stream().anyMatch(x -> x.getId() == sid), "SubTask должен попасть в prioritized");
        assertTrue(p.stream().anyMatch(x -> x.getId() == tid), "Task должен попасть в prioritized");

        int idxS = -1, idxT = -1;
        for (int i = 0; i < p.size(); i++) {
            if (p.get(i).getId() == sid) idxS = i;
            if (p.get(i).getId() == tid) idxT = i;
        }
        assertTrue(idxS >= 0 && idxT >= 0 && idxS < idxT, "SubTask с более ранним start должен быть раньше");
    }

    @Test
    void prioritizedContainsEpicAfterSubtaskRecalc() {
        int epicId = mustAddEpic(mkEpic("E"));
        assertFalse(manager.getPrioritizedTasks().stream().anyMatch(t -> t.getId() == epicId),
                "Эпик без времени не должен появляться в prioritized");

        LocalDateTime t8 = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        mustAddSub(mkSubAt("S1", epicId, t8, Duration.ofMinutes(15)));

        List<Task> p2 = manager.getPrioritizedTasks();
        assertTrue(p2.stream().anyMatch(t -> t.getId() == epicId),
                "После добавления сабтаска и пересчёта эпик должен быть в prioritized");

        int idxEpic = -1, idxSub = -1;
        for (int i = 0; i < p2.size(); i++) {
            Task tt = p2.get(i);
            if (tt.getId() == epicId) idxEpic = i;
            if (tt instanceof SubTask && ((SubTask) tt).getEpicId() == epicId) idxSub = i;
        }
        assertTrue(idxEpic >= 0 && idxSub >= 0, "Должны найти и эпик, и его сабтаск в prioritized");
        assertTrue(idxEpic <= idxSub, "Эпик должен идти не позже своего раннего сабтаска (по minStart)");
    }

    @Test
    void prioritizedZeroDurationIsIncludedAndOrderedByStart() {
        LocalDateTime t10 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime t11 = t10.plusHours(1);

        int idZero = mustAddTask(mkTaskAt("Z", t11, Duration.ZERO));            // zero → ветка без брони, но в prioritized добавляется
        int idNorm = mustAddTask(mkTaskAt("N", t10, Duration.ofMinutes(15)));

        List<Task> p = manager.getPrioritizedTasks();
        assertTrue(p.stream().anyMatch(x -> x.getId() == idZero));
        assertTrue(p.stream().anyMatch(x -> x.getId() == idNorm));

        int idxZero = -1, idxNorm = -1;
        for (int i = 0; i < p.size(); i++) {
            if (p.get(i).getId() == idZero) idxZero = i;
            if (p.get(i).getId() == idNorm) idxNorm = i;
        }
        assertTrue(idxNorm < idxZero, "Элемент с 10:00 должен идти раньше элемента с 11:00 (даже если у него Duration.ZERO)");
    }


    private int mustAddTask(Task t) {
        OptionalInt oi = manager.addTask(t);
        assertTrue(oi.isPresent(), "Ожидался id задачи");
        return oi.getAsInt();
    }
    private int mustAddEpic(Epic e) {
        OptionalInt oi = manager.addEpic(e);
        assertTrue(oi.isPresent(), "Ожидался id эпика");
        return oi.getAsInt();
    }
    private int mustAddSub(SubTask s) {
        OptionalInt oi = manager.addSubTask(s);
        assertTrue(oi.isPresent(), "Ожидался id подзадачи");
        return oi.getAsInt();
    }

    private Task mkTaskAt(String name, LocalDateTime start, Duration dur) {
        Task t = mkTask(name);
        t.setStartTime(start);
        t.setDuration(dur);
        return t;
    }
    private SubTask mkSubAt(String name, int epicId, LocalDateTime start, Duration dur) {
        SubTask st = mkSub(name, epicId);
        st.setStartTime(start);
        st.setDuration(dur);
        return st;
    }


}