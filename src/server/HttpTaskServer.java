package server;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import service.Managers;
import service.TaskManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalInt;

public class HttpTaskServer {
    public static final int PORT = 8079;

    private final TaskManager manager;
    private final HttpServer server;
    private static final DateTimeFormatter ISO_DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.format(ISO_DTF)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, t, ctx) -> LocalDateTime.parse(json.getAsString(), ISO_DTF))
            .registerTypeAdapter(Duration.class,
                    (JsonSerializer<Duration>) (src, t, ctx) -> new JsonPrimitive(src.toMinutes()))
            .registerTypeAdapter(Duration.class,
                    (JsonDeserializer<Duration>) (json, t, ctx) -> Duration.ofMinutes(json.getAsLong()))
            .create();


    public HttpTaskServer(URI kvUri) throws IOException {
        this.manager = Managers.getDefaultHTTPManager(kvUri);
        this.server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        server.createContext("/tasks/task", this::handleTask);
        server.createContext("/tasks/epic", this::handleEpic);
        server.createContext("/tasks/epic/subtasks", this::handleEpicSubtasks);
        server.createContext("/tasks/subtask", this::handleSubtask);
        server.createContext("/tasks/all", this::handleAll);
        server.createContext("/tasks/", this::handlePrioritized);
        server.createContext("/tasks/history", this::handleHistory);
        server.createContext("/tasks/task/status", this::handleTaskStatus);
        server.createContext("/tasks/subtask/status", this::handleSubtaskStatus);


    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handleHistory(HttpExchange h) throws IOException {
        if (!"GET".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }
        sendJson(h, 200, manager.getHistory());
    }

    private void handleSubtaskStatus(HttpExchange h) throws IOException {
        if (!"POST".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }
        Integer id = queryId(h);
        if (id == null) {
            sendText(h, 400, "Query param 'id' is required");
            return;
        }

        String statusStr = queryParam(h, "status");
        if (statusStr == null || statusStr.isEmpty()) {
            String raw = read(h);
            if (raw != null && !raw.isEmpty()) {
                JsonObject jo = JsonParser.parseString(raw).getAsJsonObject();
                if (jo.has("status")) statusStr = jo.get("status").getAsString();
            }
        }
        if (statusStr == null || statusStr.isEmpty()) {
            sendText(h, 400, "'status' is required");
            return;
        }

        Status st;
        try {
            st = Status.valueOf(statusStr);
        } catch (IllegalArgumentException ex) {
            sendText(h, 400, "Unknown status. Use NEW | IN_PROGRESS | DONE");
            return;
        }

        model.SubTask s = manager.getSubTask(id);
        if (s == null) {
            sendText(h, 404, "SubTask not found");
            return;
        }

        manager.setSubTaskStatus(id, st);
        sendJson(h, 200, manager.getSubTask(id));
    }

    private static String queryParam(HttpExchange h, String key) {
        String q = h.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String p : q.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private void handleTaskStatus(HttpExchange h) throws IOException {
        if (!"POST".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }
        Integer id = queryId(h);
        if (id == null) {
            sendText(h, 400, "Query param 'id' is required");
            return;
        }

        String statusStr = queryParam(h, "status");
        if (statusStr == null || statusStr.isEmpty()) {
            String raw = read(h);
            if (raw != null && !raw.isEmpty()) {
                JsonObject jo = JsonParser.parseString(raw).getAsJsonObject();
                if (jo.has("status")) statusStr = jo.get("status").getAsString();
            }
        }
        if (statusStr == null || statusStr.isEmpty()) {
            sendText(h, 400, "'status' is required");
            return;
        }

        Status st;
        try {
            st = Status.valueOf(statusStr);
        } catch (IllegalArgumentException ex) {
            sendText(h, 400, "Unknown status. Use NEW | IN_PROGRESS | DONE");
            return;
        }

        Task t = manager.getTask(id);
        if (t == null) {
            sendText(h, 404, "Task not found");
            return;
        }

        manager.setTaskStatus(id, st);
        sendJson(h, 200, manager.getTask(id));
    }

    private void handlePrioritized(HttpExchange h) throws IOException {
        if (!"GET".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }
        sendJson(h, 200, manager.getPrioritizedTasks());
    }


    private void handleTask(HttpExchange h) throws IOException {

        String m = h.getRequestMethod();
        Integer id = queryId(h);
        switch (m) {
            case "GET" -> {
                if (id == null) sendJson(h, 200, manager.getAllTasks());
                else {
                    Task t = manager.getTask(id);
                    if (t == null) sendText(h, 404, "");
                    else sendJson(h, 200, t);
                }
            }
            case "POST" -> {
                String raw = read(h);
                JsonObject jo = JsonParser.parseString(raw).getAsJsonObject();

                Task body = new Task();
                if (jo.has("name")) body.setName(jo.get("name").getAsString());
                if (jo.has("description")) body.setDescription(jo.get("description").getAsString());
                if (jo.has("status")) body.setStatus(Status.valueOf(jo.get("status").getAsString()));
                if (jo.has("startTime") && !jo.get("startTime").isJsonNull()) {
                    body.setStartTime(LocalDateTime.parse(jo.get("startTime").getAsString(), ISO_DTF));
                }
                if (jo.has("duration") && !jo.get("duration").isJsonNull()) {
                    body.setDuration(Duration.ofMinutes(jo.get("duration").getAsLong()));
                }

                if (id == null) {
                    OptionalInt newId = manager.addTask(body);
                    if (newId.isEmpty()) {
                        sendText(h, 409, "");
                        return;
                    }
                    sendJson(h, 201, manager.getTask(newId.getAsInt()));
                } else {
                    int useId = id;
                    boolean ok = manager.updateTask(useId, body);
                    if (!ok) {
                        sendText(h, 404, "");
                        return;
                    }
                    sendJson(h, 200, manager.getTask(useId));
                }
            }
            case "DELETE" -> {
                if (id == null) manager.deleteAllTasks();
                else manager.deleteTask(id);
                sendText(h, 200, "OK");
            }
            default -> sendText(h, 405, "");
        }
    }

    private void handleAll(HttpExchange h) throws IOException {
        if (!"GET".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }

        JsonObject out = new JsonObject();
        out.add("tasks", gson.toJsonTree(manager.getAllTasks()));
        out.add("epics", gson.toJsonTree(manager.getAllEpics()));
        out.add("subtasks", gson.toJsonTree(manager.getAllSubTasks()));

        sendJson(h, 200, out);
    }


    private void handleEpic(HttpExchange h) throws IOException {

        String m = h.getRequestMethod();
        Integer id = queryId(h);
        switch (m) {
            case "GET" -> {
                if (id == null) sendJson(h, 200, manager.getAllEpics());
                else {
                    Epic t = manager.getEpic(id);
                    if (t == null) sendText(h, 404, "");
                    else sendJson(h, 200, t);
                }
            }
            case "POST" -> {
                String raw = read(h);
                JsonObject jo = JsonParser.parseString(raw).getAsJsonObject();

                Epic body = new Epic();
                if (jo.has("name")) body.setName(jo.get("name").getAsString());
                if (jo.has("description")) body.setDescription(jo.get("description").getAsString());
                if (jo.has("status")) body.setStatus(Status.valueOf(jo.get("status").getAsString()));
                if (jo.has("startTime") && !jo.get("startTime").isJsonNull()) {
                    body.setStartTime(LocalDateTime.parse(jo.get("startTime").getAsString(), ISO_DTF));
                }
                if (jo.has("duration") && !jo.get("duration").isJsonNull()) {
                    body.setDuration(Duration.ofMinutes(jo.get("duration").getAsLong()));
                }

                if (id == null) {
                    OptionalInt newId = manager.addEpic(body);
                    if (newId.isEmpty()) {
                        sendText(h, 409, "");
                        return;
                    }
                    sendJson(h, 201, manager.getEpic(newId.getAsInt()));
                } else {
                    int useId = id;
                    boolean ok = manager.updateEpic(useId, body);
                    if (!ok) {
                        sendText(h, 404, "");
                        return;
                    }
                    sendJson(h, 200, manager.getEpic(useId));
                }
            }
            case "DELETE" -> {
                if (id == null) manager.deleteAllEpics();
                else manager.deleteEpic(id);
                sendText(h, 200, "OK");
            }
            default -> sendText(h, 405, "");
        }
    }

    private void handleEpicSubtasks(HttpExchange h) throws IOException {
        if (!"GET".equals(h.getRequestMethod())) {
            sendText(h, 405, "");
            return;
        }

        Integer epicId = queryId(h);
        if (epicId == null) {
            sendText(h, 400, "Неверный параметр");
            return;
        }

        Epic epic = manager.getEpic(epicId);
        if (epic == null) {
            sendText(h, 404, "Epic не найден");
            return;
        }


        List<SubTask> result = manager.getEpicsSubTasks(epicId);

        sendJson(h, 200, result);
    }

    private void handleSubtask(HttpExchange h) throws IOException {
        String m = h.getRequestMethod();
        Integer id = queryId(h);

        switch (m) {
            case "GET" -> {
                if (id == null) {
                    sendJson(h, 200, manager.getAllSubTasks());
                } else {
                    model.SubTask t = manager.getSubTask(id);
                    if (t == null) {
                        sendText(h, 404, "");
                    } else {
                        sendJson(h, 200, t);
                    }
                }
            }

            case "POST" -> {
                String raw = read(h);
                JsonObject jo = JsonParser.parseString(raw).getAsJsonObject();

                model.SubTask body = new model.SubTask();


                if (jo.has("name")) body.setName(jo.get("name").getAsString());
                if (jo.has("description")) body.setDescription(jo.get("description").getAsString());
                if (jo.has("status")) body.setStatus(Status.valueOf(jo.get("status").getAsString()));

                if (jo.has("startTime") && !jo.get("startTime").isJsonNull()) {
                    body.setStartTime(LocalDateTime.parse(jo.get("startTime").getAsString(), ISO_DTF));
                }
                if (jo.has("duration") && !jo.get("duration").isJsonNull()) {
                    body.setDuration(Duration.ofMinutes(jo.get("duration").getAsLong()));
                }

                if (id == null) {
                    if (!jo.has("epicId")) {
                        sendText(h, 400, "'epicId' is required");
                        return;
                    }
                    int epicId = jo.get("epicId").getAsInt();


                    Epic ep = manager.getEpic(epicId);
                    if (ep == null) {
                        sendText(h, 404, "Epic not found");
                        return;
                    }
                    body.setEpicId(epicId);

                    OptionalInt newId = manager.addSubTask(body);
                    if (newId.isEmpty()) {
                        sendText(h, 409, "");
                        return;
                    }
                    sendJson(h, 201, manager.getSubTask(newId.getAsInt()));

                } else {
                    if (jo.has("epicId") && !jo.get("epicId").isJsonNull()) {
                        int epicId = jo.get("epicId").getAsInt();
                        Epic ep = manager.getEpic(epicId);
                        if (ep == null) {
                            sendText(h, 404, "Epic not found");
                            return;
                        }
                        body.setEpicId(epicId);
                    }

                    boolean ok = manager.updateSubTask(id, body);
                    if (!ok) {
                        sendText(h, 404, "");
                        return;
                    }
                    sendJson(h, 200, manager.getSubTask(id));
                }
            }

            case "DELETE" -> {
                if (id == null) {
                    manager.deleteAllSubTasks();
                } else {
                    manager.deleteSubtask(id);
                }
                sendText(h, 200, "OK");
            }

            default -> sendText(h, 405, "");
        }
    }


    private void sendJson(HttpExchange h, int code, Object data) throws IOException {
        byte[] resp = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        h.sendResponseHeaders(code, resp.length);
        try (OutputStream os = h.getResponseBody()) {
            os.write(resp);
        }
    }

    private void sendText(HttpExchange h, int code, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        h.sendResponseHeaders(code, resp.length);
        try (OutputStream os = h.getResponseBody()) {
            os.write(resp);
        }
    }

    private static Integer queryId(HttpExchange h) {
        String q = h.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String p : q.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals("id")) {
                try {
                    return Integer.parseInt(kv[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static String read(HttpExchange h) throws java.io.IOException {
        return new String(h.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

}