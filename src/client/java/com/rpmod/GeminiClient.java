package com.rpmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class GeminiClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class GeminiException extends RuntimeException {
        public GeminiException(String message) {
            super(message);
        }
    }

    private GeminiClient() {}

    public static CompletableFuture<String> generate(RpConfig config, String system, String user) {
        String key = config.apiKey == null ? "" : config.apiKey.trim();
        if (key.isEmpty()) {
            return CompletableFuture.failedFuture(new GeminiException(
                    "Ключ API не задан. Откройте файл config/rpmod.json и впишите ключ в поле apiKey."));
        }
        String model = config.model == null ? "" : config.model.trim();
        if (model.isEmpty()) model = "gemini-2.5-flash";

        JsonObject body = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", parts(system));
        body.add("system_instruction", systemInstruction);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.add("parts", parts(user));
        JsonArray contents = new JsonArray();
        contents.add(userMessage);
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 1024);
        body.add("generationConfig", generationConfig);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-goog-api-key", key)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new GeminiException(
                    "Не удалось собрать запрос. Проверьте ключ (apiKey) и название модели (model) в config/rpmod.json: "
                            + "в них не должно быть пробелов и русских букв."));
        }

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> parse(r.statusCode(), r.body()));
    }

    private static JsonArray parts(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray arr = new JsonArray();
        arr.add(part);
        return arr;
    }

    private static String parse(int status, String body) {
        if (status != 200) {
            throw new GeminiException(humanError(status, body));
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new GeminiException("Gemini не вернул ответ (возможно, запрос заблокирован фильтром). Попробуйте переформулировать подсказку.");
            }
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            StringBuilder sb = new StringBuilder();
            if (content != null && content.has("parts")) {
                for (var el : content.getAsJsonArray("parts")) {
                    JsonObject part = el.getAsJsonObject();
                    if (part.has("text")) sb.append(part.get("text").getAsString());
                }
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new GeminiException("Ответ пустой. Возможно, не хватило токенов или сработал фильтр. Нажмите «Переделать».");
            }
            return text;
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Не удалось разобрать ответ Gemini: " + e.getMessage());
        }
    }

    private static String humanError(int status, String body) {
        String details = "";
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("error")) {
                JsonObject err = root.getAsJsonObject("error");
                if (err.has("message")) details = err.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        if (details.length() > 180) details = details.substring(0, 180) + "…";

        String hint;
        switch (status) {
            case 400 -> hint = "Запрос отклонён. Проверьте ключ API (apiKey) и название модели (model) в config/rpmod.json.";
            case 401, 403 -> hint = "Ключ не принят. Проверьте apiKey в config/rpmod.json: он скопирован целиком, без пробелов?";
            case 404 -> hint = "Модель не найдена. Проверьте название модели (model) в config/rpmod.json.";
            case 429 -> hint = "Слишком много запросов или кончился лимит. Подождите минуту.";
            case 500, 503 -> hint = "Сервер Gemini перегружен. Попробуйте ещё раз через несколько секунд.";
            default -> hint = "Проверьте ключ API и название модели в config/rpmod.json.";
        }
        return "Ошибка Gemini, код " + status + ". " + hint + (details.isEmpty() ? "" : " (" + details + ")");
    }
}
