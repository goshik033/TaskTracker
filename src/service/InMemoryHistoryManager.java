package service;

import model.Task;

import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private static class Node {
        Node next;
        Node prev;
        Task task;

        Node(Task task) {
            this.task = task;
        }
    }

    private final Map<Integer, Node> map = new HashMap<>();
    //    private final Deque<Task> history = new ArrayDeque<>();
    private final static int HISTURY_SIZE = 10;
    private Node head;
    private Node tail;

    @Override
    public void add(Task task) {
        if (task == null) return;
        int id = task.getId();
        Node oldNode = map.remove(id);
        if (oldNode != null)
            removeNode(oldNode);
        addNode(task);
    }
    @Override
    public void remove(int id) {
        Node node = map.remove(id);
        if (node != null) removeNode(node);
    }

    private void addNode(Task task) {
        if (tail == null) {
            tail = new Node(task);
            head = tail;
            map.put(task.getId(), tail);
        } else {
            tail.next = new Node(task);
            tail.next.prev = tail;
            tail = tail.next;
            map.put(task.getId(), tail);
        }
    }

    private void removeNode(Node oldNode) {
        Node n = oldNode.next;
        Node p = oldNode.prev;
        if (p != null) {
            p.next = n;
        } else {
            head = n;
        }
        if (n != null) {
            n.prev = p;
        } else {
            tail = p;
        }
        oldNode.prev = null;
        oldNode.next = null;
        oldNode.task = null;
    }

    @Override
    public List<Task> getHistory() {
        List<Task> tasks = new ArrayList<>();
        Node h = head;
        while (h != null) {
            tasks.add(h.task);
            h = h.next;
        }
        return tasks;
    }
}
