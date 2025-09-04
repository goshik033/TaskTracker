package service;

import model.Task;

import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private final Deque<Task> history = new ArrayDeque<>();
    private final static int HISTURY_SIZE = 10;

    @Override
    public void add(Task task) {
        if (task == null) return;
        if (history.size() == HISTURY_SIZE) {
            history.removeFirst();
        }
        history.addFirst( task);
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}
