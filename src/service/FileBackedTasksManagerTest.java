package service;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class FileBackedTasksManagerTest extends TaskManagerTest<TaskManager> {

    @TempDir
    Path tmpDir;

    @Override
    protected TaskManager createManager() {
        Path file = tmpDir.resolve("test.csv");
        return FileBackedTasksManager.loadFromFile(file);
    }
}
