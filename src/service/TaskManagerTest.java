package service;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    private void addSub(Status status,int epicId) {
        SubTask st = new SubTask();
        st.setEpicId(epicId);
        int sid = manager.addSubTask(st);
        manager.setSubTaskStatus(sid, status);
    }

    @Test
    void addAndGetTaskStandard() {
        int id = manager.addTask(mkTask("T1"));
        Task got = manager.getTask(id);
        assertNotNull(got);
        assertEquals("T1", got.getName());
        assertTrue(manager.getAllTasks().stream().anyMatch(t -> t.getId() == id));
    }
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
        int epicId = manager.addEpic(mkEpic("E1"));
        int subId = manager.addSubTask(mkSub("S1", epicId));
        SubTask s = manager.getSubTask(subId);
        assertNotNull(s);
        assertEquals(epicId, s.getEpicId());
        assertTrue(manager.getEpicsSubTasks(epicId).stream().anyMatch(x -> x.getId() == subId));
    }

    @Test
    void addSubTaskWrongEpicThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(mkSub("S1", 123456)));
    }
    @Test
    void epicStatusEmptySubtasksisNEW() {
        int epicId = manager.addEpic(mkEpic("E1"));
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());

    }

    @Test
    void EpicStatusAllSubtasksNEWisNEW() {
        int epicId = manager.addEpic(mkEpic("E1"));

        addSub(Status.NEW,epicId);
        addSub(Status.NEW,epicId);
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());
    }

    @Test
    void EpicStatusAllSubtasksDONEisDONE() {
        int epicId = manager.addEpic(mkEpic("E1"));

        addSub(Status.DONE,epicId);
        addSub(Status.DONE,epicId);
        assertEquals(Status.DONE, manager.getEpic(epicId).getStatus());
    }
    @Test
    void EpicStatusSubtasksMixedDONEandNEWisIN_PROGRESS() {
        int epicId = manager.addEpic(mkEpic("E1"));

        addSub(Status.NEW,epicId);
        addSub(Status.DONE,epicId);
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }
    @Test
    void EpicStatusSubtasksHasIN_PROGRESSisIN_PROGRESS() {
        int epicId = manager.addEpic(mkEpic("E1"));
        addSub(Status.NEW,epicId);
        addSub(Status.NEW,epicId);
        addSub(Status.IN_PROGRESS,epicId);
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }



    @Test
    void deleteAllSubTasksResetsEpicStatusToNEW() {
        int epicId = manager.addEpic(mkEpic("E1"));
        addSub(Status.DONE,epicId);
        manager.deleteAllSubTasks();
        assertTrue(manager.getEpicsSubTasks(epicId).isEmpty());
        assertEquals(Status.NEW, manager.getEpic(epicId).getStatus());
    }

    @Test
    void deleteEpicRemovesItsSubtasks() {
        int epicId = manager.addEpic(mkEpic("E1"));
        int s1 = manager.addSubTask(mkSub("S1", epicId));
        manager.deleteEpic(epicId);
        assertNull(manager.getEpic(epicId));
        assertNull(manager.getSubTask(s1));
    }


}