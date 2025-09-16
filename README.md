# TaskTracker 

Трекер задач с REST-API, историей просмотров и тремя вариантами хранения:
- **In-Memory** — всё в памяти;
- **CSV** — файл через `FileBackedTasksManager`;
- **KV** — удалённое хранение через `KVServer` + `KVTaskClient`/`HTTPTaskManager`.

Проект написан на Java

