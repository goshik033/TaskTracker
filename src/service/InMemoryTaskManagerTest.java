package service;

class InMemoryTaskManagerTest extends TaskManagerTest<TaskManager> {
    @Override
    protected TaskManager createManager() {
        return new InMemoryTaskManager();
    }
}
