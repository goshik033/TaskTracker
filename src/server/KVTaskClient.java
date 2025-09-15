package server;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class KVTaskClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;
    private final String apiToken;

    public KVTaskClient(URI serverUri) {
        this(serverUri, false);
    }

    public KVTaskClient(URI serverUri, boolean debug) {
        String s = serverUri.toString();
        this.base = s.endsWith("/") ? s : s + "/";
        this.apiToken = debug ? "DEBUG" : register();
    }

    private String register() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "register"))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("register failed: " + resp.statusCode());
            }
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("register error", e);
        }
    }

    public void put(String key, String json) {
        try {
            String url = base + "save/" + encode(key) + "?API_TOKEN=" + apiToken;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("save failed: " + resp.statusCode() + " " + resp.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("save error", e);
        }
    }

    public String get(String key) {
        try {
            String url = base + "load/" + encode(key) + "?API_TOKEN=" + apiToken;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) return null;
            if (resp.statusCode() != 200) {
                throw new RuntimeException("load failed: " + resp.statusCode() + " " + resp.body());
            }
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("load error", e);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
