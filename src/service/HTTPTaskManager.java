package service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.Epic;
import model.SubTask;
import model.Task;
import server.KVTaskClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HTTPTaskManager extends FileBackedTasksManager {

    private static final String K_TASKS = "tasks";
    private static final String K_EPICS = "epics";
    private static final String K_SUBS  = "subtasks";
    private static final String K_HIST  = "history";

    private final KVTaskClient kv;
    private final Gson gson;
    private boolean loading = false;

    public HTTPTaskManager(URI kvServer) {
        super(Path.of("http-placeholder.csv")); // файл не используется
        this.kv = new KVTaskClient(kvServer);
        DateTimeFormatter ISO_DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.format(ISO_DTF)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (j, t, ctx) -> LocalDateTime.parse(j.getAsString(), ISO_DTF))
                .registerTypeAdapter(Duration.class,
                        (JsonSerializer<Duration>) (src, t, ctx) -> new JsonPrimitive(src == null ? null : src.toMinutes()))
                .registerTypeAdapter(Duration.class,
                        (JsonDeserializer<Duration>) (j, t, ctx) -> j.isJsonNull() ? null : Duration.ofMinutes(j.getAsLong()))
                .create();
    }

    @Override
    protected void save() {
        if (loading) return;
        kv.put(K_TASKS, gson.toJson(getAllTasks()));
        kv.put(K_EPICS, gson.toJson(getAllEpics()));
        kv.put(K_SUBS,  gson.toJson(getAllSubTasks()));
        kv.put(K_HIST,  gson.toJson(getHistory().stream().map(Task::getId).toList()));
    }

    public static HTTPTaskManager load(URI kvServer) {
        HTTPTaskManager m = new HTTPTaskManager(kvServer);

        Type T_TASKS = new TypeToken<List<Task>>(){}.getType();
        Type T_EPICS = new TypeToken<List<Epic>>(){}.getType();
        Type T_SUBS  = new TypeToken<List<SubTask>>(){}.getType();
        Type T_IDS   = new TypeToken<List<Integer>>(){}.getType();

        m.loading = true;
        try {
            String jsEpics = m.kv.get(K_EPICS);
            String jsTasks = m.kv.get(K_TASKS);
            String jsSubs  = m.kv.get(K_SUBS);
            String jsHist  = m.kv.get(K_HIST);

            if (jsEpics != null) {
                List<Epic> epics = m.gson.fromJson(jsEpics, T_EPICS);
                if (epics != null) {
                    for (Epic e : epics) m.addEpic(e);
                }
            }

            if (jsTasks != null) {
                List<Task> tasks = m.gson.fromJson(jsTasks, T_TASKS);
                if (tasks != null) {
                    for (Task t : tasks) m.addTask(t);
                }
            }

            if (jsSubs != null) {
                List<SubTask> subs = m.gson.fromJson(jsSubs, T_SUBS);
                if (subs != null) {
                    for (SubTask s : subs) m.addSubTask(s);
                }
            }

            if (jsHist != null) {
                List<Integer> ids = m.gson.fromJson(jsHist, T_IDS);
                if (ids != null) {
                    for (Integer id : ids) {
                        if (m.getTask(id) == null && m.getEpic(id) == null) m.getSubTask(id);
                    }
                }
            }

        } finally {
            m.loading = false;
            m.save();
        }
        return m;
    }
}
