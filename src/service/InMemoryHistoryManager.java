package service;

import model.Task;

import java.util.ArrayList;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> history = new ArrayList<>();

    private final int histurySize = 10;
    private int idx = 0;

    @Override
    public void add(Task task) {
        history.add(idx, task);
        idx = (1 + idx) % histurySize;
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}
