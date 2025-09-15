package service;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import static service.FileBackedTasksManager.loadFromFile;

public class Managers {

    private Managers() {
    }

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }
    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
    public static TaskManager getDefaultFileManager(Path path) {
        return FileBackedTasksManager.loadFromFile(path);
    } public static TaskManager getDefaultHTTPManager(URI kvUri) {
        return HTTPTaskManager.load(kvUri);
    }
}
