package com.rpmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class RpModClient implements ClientModInitializer {

    /** Этот класс = содержимое файла config/rpmod.json */
    public static class Config {
        public String apiKey = "ВСТАВЬ_КЛЮЧ_СЮДА";
        public String model = "gemini-2.5-flash";
        public String characterName = "Имя персонажа";
        public String character = "Опиши здесь своего персонажа: дом, род занятий, характер, как он держится и говорит.";
        // plain = просто текст, asterisks = *текст*, me = команда /me текст
        public String actionFormat = "plain";
    }

    public static Config config = new Config();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.rpmod.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.rpmod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    loadConfig(); // перечитываем файл, чтобы правки работали без перезапуска игры
                    client.setScreen(new RpScreen());
                }
            }
        });
    }

    public static void loadConfig() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("rpmod.json");
        try {
            if (Files.exists(path)) {
                Config loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Config.class);
                if (loaded != null) config = loaded;
            } else {
                config = new Config();
                Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject textContent(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        return content;
    }

    public static CompletableFuture<String> generate(String hint) {
        String system = "Ты помогаешь игроку писать РП-действия для Minecraft-сервера по «Игре Престолов». "
                + "Персонаж игрока: " + config.characterName + ". " + config.character + " "
                + "Напиши ОДНО РП-действие этого персонажа по подсказке игрока. Правила: "
                + "от третьего лица, в настоящем времени, 1–2 коротких предложения (до 200 символов), "
                + "литературный стиль Вестероса, без кавычек, без пояснений и вступлений. "
                + "Не описывай действия и реплики других игроков, не выдумывай важных фактов о мире. "
                + "Пример: «Прищурившись, наблюдает за торговцем из-под капюшона.»";

        JsonObject body = new JsonObject();
        body.add("system_instruction", textContent(system));

        JsonObject userContent = textContent("Подсказка игрока: " + hint);
        userContent.addProperty("role", "user");
        JsonArray contents = new JsonArray();
        contents.add(userContent);
        body.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 1.0);
        genConfig.addProperty("maxOutputTokens", 1024);
        body.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models/" + config.model + ":generateContent"))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", config.apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("код ответа " + response.statusCode()
                                + " (проверь ключ и название модели в rpmod.json)");
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    return json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                            .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                            .get("text").getAsString().trim();
                });
    }

    public static void sendToChat(Minecraft mc, String text) {
        if (mc.player == null) return;
        text = text.trim();
        switch (config.actionFormat) {
            case "me" -> mc.player.connection.sendCommand("me " + cut(text, 240));
            case "asterisks" -> mc.player.connection.sendChat("*" + cut(text, 250) + "*");
            default -> mc.player.connection.sendChat(cut(text, 256));
        }
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
