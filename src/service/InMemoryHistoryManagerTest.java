package service;

import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    protected InMemoryHistoryManager history;
    protected InMemoryTaskManager tm;

    @BeforeEach
    void setUp() {
        history = new InMemoryHistoryManager();
        tm = new InMemoryTaskManager();
    }

    @Test
    void getHistoryEmpty() {
        assertNotNull(history.getHistory());
        assertTrue(history.getHistory().isEmpty());
    }

    @Test
    void addTaskToHistory() {
        Task t = new Task();
        tm.addTask(t);
        history.add(t);
        assertNotNull(history.getHistory());
        assertEquals(1, history.getHistory().size());
        assertTrue(history.getHistory().stream().anyMatch(x -> x.getId() == t.getId()));
    }

    @Test
    void addDublicateToHistory() {
        Task a = new Task();
        Task b = new Task();
        tm.addTask(a);
        tm.addTask(b);
        history.add(a);
        history.add(b);
        history.add(a);
        List<Task> h = history.getHistory();
        assertNotNull(h);
        assertEquals(2, h.size());
        assertEquals(b.getId(), h.get(0).getId());
        assertEquals(a.getId(), h.get(1).getId());
    }

    @Test
    void removeHead() {
        List<Integer> ids = addTasks();
        history.remove(0);
        List<Task> h = history.getHistory();
        assertEquals(2, h.size());
        assertEquals(ids.get(1), h.get(0).getId());
        assertEquals(ids.get(2), h.get(1).getId());

    }

    @Test
    void removeTail() {
        List<Integer> ids = addTasks();
        history.remove(2);
        List<Task> h = history.getHistory();
        assertEquals(2, h.size());
        assertEquals(ids.get(0), h.get(0).getId());
        assertEquals(ids.get(1), h.get(1).getId());

    }

    @Test
    void removeFromMiddle() {
        List<Integer> ids = addTasks();
        history.remove(1);
        List<Task> h = history.getHistory();
        assertEquals(2, h.size());
        assertEquals(ids.get(0), h.get(0).getId());
        assertEquals(ids.get(2), h.get(1).getId());

    }

    private List<Integer> addTasks() {
        Task f = new Task();
        Task s = new Task();
        Task t = new Task();
        tm.addTask(f);
        tm.addTask(s);
        tm.addTask(t);
        history.add(f);
        history.add(s);
        history.add(t);

        return new ArrayList<>(List.of(f.getId(), s.getId(), t.getId()));


    }


}