package app;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import service.Managers;
import service.TaskManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final Path PATH = Paths.get("tasks.csv");
    private static final TaskManager manager = Managers.getDefaultFileManager(PATH);

    public static void main(String[] args) {
        while (true) {
            printMenu();
            int cmd = readInt("Выберите пункт: ");
            switch (cmd) {
                case 1 -> createTask();
                case 2 -> createEpic();
                case 3 -> createSubtask();
                case 4 -> listAll();
                case 5 -> listEpicSubtasks();
                case 6 -> updateTask();
                case 7 -> updateEpic();
                case 8 -> updateSubtask();
                case 9 -> deleteById();
                case 10 -> deleteAll();
                case 11 -> setStatus();
                case 12 -> showById();
                case 13 -> getHistory();
                case 0 -> {
                    System.out.println("Пока!");
                    return;
                }
                default -> System.out.println("Нет такого пункта, попробуйте ещё раз.");
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("=== Меню ===");
        System.out.println("1. Добавить Task");
        System.out.println("2. Добавить Epic");
        System.out.println("3. Добавить SubTask к эпику");
        System.out.println("4. Показать все: Tasks / Epics / SubTasks");
        System.out.println("5. Показать подзадачи эпика");
        System.out.println("6. Обновить Task");
        System.out.println("7. Обновить Epic");
        System.out.println("8. Обновить SubTask");
        System.out.println("9. Удалить по id ( Task/ Epic/ SubTask)");
        System.out.println("10. Очистить всё (Tasks/Epics/SubTasks)");
        System.out.println("11. Изменить статус (Tasks/SubTasks)");
        System.out.println("12. Показать по id ( Task/ Epic/ SubTask)");
        System.out.println("13. Показать историю");
        System.out.println("0. Выход");
    }

    private static void getHistory() {
        List<Task> history = manager.getHistory();
        if (history == null || history.isEmpty()) {
            System.out.println("история пуста");
            return;
        }
        System.out.println(history);
    }


    private static void createTask() {
        Task t = new Task();
        t.setName(readLine("Название: "));
        t.setDescription(readLine("Описание: "));
        int id = manager.addTask(t);
        System.out.println(" Task создан, id=" + id);
    }

    private static void createEpic() {
        Epic e = new Epic();
        e.setName(readLine("Название эпика: "));
        e.setDescription(readLine("Описание: "));
        int id = manager.addEpic(e);
        System.out.println(" Epic создан, id=" + id);
    }

    private static void createSubtask() {
        int epicId = readInt(" Epic id: ");
        SubTask s = new SubTask();
        s.setEpicId(epicId);
        s.setName(readLine("Название подзадачи: "));
        s.setDescription(readLine("Описание: "));
        int id = manager.addSubTask(s);
        System.out.println(" SubTask создан, id=" + id);
    }


    private static void listAll() {
        System.out.println("-- Tasks --");
        printList(manager.getAllTasks());
        System.out.println("-- Epics --");
        printList(manager.getAllEpics());
        System.out.println("-- SubTasks --");
        printList(manager.getAllSubTasks());
    }

    private static void listEpicSubtasks() {
        int epicId = readInt(" Epic id: ");
        List<SubTask> list = manager.getEpicsSubTasks(epicId);
        if (list.isEmpty()) System.out.println("Нет подзадач или эпик не найден.");
        else printList(list);
    }


    // -------- UPDATE --------
    private static void updateTask() {
        int id = readInt("Task id: ");
        Task t = new Task();
        t.setName(readLine("Новое название: "));
        t.setDescription(readLine("Новое описание: "));
        boolean ok = manager.updateTask(id, t);
        System.out.println(ok ? "Обновлено." : "Task не найден.");
    }

    private static void updateEpic() {
        int id = readInt("Epic id: ");
        Epic e = new Epic();
        e.setName(readLine("Новое название: "));
        e.setDescription(readLine("Новое описание: "));
        boolean ok = manager.updateEpic(id, e);
        System.out.println(ok ? "Обновлено." : "Epic не найден.");
    }

    private static void updateSubtask() {
        int id = readInt("SubTask id: ");
        SubTask s = new SubTask();
        s.setName(readLine("Новое название: "));
        s.setDescription(readLine("Новое описание: "));
        boolean ok = manager.updateSubTask(id, s);
        System.out.println(ok ? "Обновлено." : "SubTask не найден.");
    }

    // -------- DELETE --------
    private static void deleteById() {
        int type = readInt("Что удалить? 1-Task, 2-Epic, 3-SubTask: ");
        int id = readInt("id: ");
        switch (type) {
            case 1 -> manager.deleteTask(id);
            case 2 -> manager.deleteEpic(id);
            case 3 -> manager.deleteSubtask(id);
            default -> System.out.println("Неизвестный тип.");
        }
        System.out.println("Готово.");
    }

    private static void showById() {
        int type = readInt("Что посмотреть ? 1-Task, 2-Epic, 3-SubTask: ");
        int id = readInt("id: ");
        switch (type) {
            case 1 -> System.out.println(manager.getTask(id));
            case 2 -> System.out.println(manager.getEpic(id));
            case 3 -> System.out.println(manager.getSubTask(id));
            default -> System.out.println("Неизвестный тип.");
        }

        System.out.println("Готово.");
    }

    private static void setStatus() {
        int type = readInt("Кому изменить статус? 1-Task, 2-SubTask: ");
        int id = readInt("id: ");
        switch (type) {
            case 1 -> {
                Status status = readStatus("На какой статус изменить? 1-NEW, 2-IN_PROGRESS, 3-DONE: ");
                manager.setTaskStatus(id, status);
            }
            case 2 -> {
                Status status = readStatus("На какой статус изменить? 1-NEW, 2-IN_PROGRESS, 3-DONE: ");
                manager.setSubTaskStatus(id, status);
            }
            default -> System.out.println("Неизвестный тип.");
        }
        System.out.println("Готово.");
    }


    private static void deleteAll() {
        int type = readInt("Что очистить? 1-Tasks, 2-Epics (с подзадачами), 3-SubTasks: ");
        switch (type) {
            case 1 -> manager.deleteAllTasks();
            case 2 -> manager.deleteAllEpics();
            case 3 -> manager.deleteAllSubTasks();
            default -> System.out.println("Неизвестный тип.");
        }
        System.out.println("Готово.");
    }

    // -------- Helpers --------
    private static void printList(List<?> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("(пусто)");
            return;
        }
        for (Object o : list) System.out.println(o);
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Введите число.");
            }
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }

    private static Status readStatus(String prompt) {
        while (true) {
            int idx = readInt(prompt);
            switch (idx) {
                case 1:
                    return Status.NEW;
                case 2:
                    return Status.IN_PROGRESS;
                case 3:
                    return Status.DONE;
                default:
                    System.out.println("1, 2 или 3.");
            }
        }
    }
}
