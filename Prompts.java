package com.rpmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Prompts {
    private Prompts() {}

    /** Общая часть для всех запросов: роль, персонаж и канон. */
    public static String base(RpConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты помогаешь игроку на ролевом сервере Minecraft по мотивам «Игры Престолов» (мир Вестероса). ");
        sb.append("Ты пишешь от лица персонажа игрока. Имя персонажа: ").append(c.characterName).append(". ");
        sb.append("Описание персонажа: ").append(c.character).append("\n");
        String canon = readCanon();
        if (!canon.isBlank()) {
            sb.append("\nКАНОН ПЕРСОНАЖА (ему нельзя противоречить):\n").append(canon).append("\n");
        }
        return sb.toString();
    }

    public static String actionSystem(RpConfig c) {
        return base(c)
                + "\nЗАДАЧА: по короткой подсказке игрока напиши ОДНО ролевое действие персонажа.\n"
                + "ПРАВИЛА:\n"
                + "- Третье лицо, настоящее время.\n"
                + "- 1–2 коротких предложения, не больше 200 символов.\n"
                + "- Стиль Вестероса: сдержанно, без современных слов.\n"
                + "- Начинай сразу с глагола, без имени персонажа.\n"
                + "- Без кавычек, без звёздочек, без пояснений и без вариантов на выбор.\n"
                + "- Не описывай действия, слова и мысли других игроков.\n"
                + "- Не пиши речь вслух: весь разговор идёт голосом, нужно только действие.\n";
    }

    public static String actionUser(String hint, String previous, String scene) {
        StringBuilder sb = new StringBuilder();
        sb.append("Подсказка игрока: ").append(hint);
        if (scene != null && !scene.isBlank()) {
            sb.append("\n").append(scene);
            sb.append("\n(Используй сцену только если это уместно, не перечисляй её целиком.)");
        }
        if (previous != null && !previous.isBlank()) {
            sb.append("\nПредыдущий вариант не подошёл: ").append(previous).append("\nНапиши другой.");
        }
        return sb.toString();
    }

    /** Данные, которые видны мне в клиенте: биом, время, погода, предмет в руке. */
    public static String scene(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return "";
        StringBuilder sb = new StringBuilder("Сцена: ");

        w.getBiome(p.getBlockPos()).getKey().ifPresent(k -> {
            Identifier id = k.getValue();
            String name = Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString();
            sb.append("биом — ").append(name).append("; ");
        });

        long t = w.getTimeOfDay() % 24000L;
        String part;
        if (t < 6000) part = "утро";
        else if (t < 12000) part = "день";
        else if (t < 13800) part = "закат";
        else if (t < 22300) part = "ночь";
        else part = "рассвет";
        sb.append("время суток — ").append(part).append("; ");

        String weather = w.isThundering() ? "гроза" : (w.isRaining() ? "дождь" : "ясно");
        sb.append("погода — ").append(weather).append("; ");

        ItemStack stack = p.getMainHandStack();
        if (stack.isEmpty()) sb.append("в руках пусто.");
        else sb.append("в руке — ").append(stack.getName().getString()).append(".");
        return sb.toString();
    }

    /** Убирает кавычки, звёздочки и переносы строк. */
    public static String cleanAction(String s) {
        String t = s.replace('\r', ' ').replace('\n', ' ').replace("*", "");
        t = t.replaceAll("\\s{2,}", " ").trim();
        String quotes = "\"«»“”'";
        while (t.length() > 1 && quotes.indexOf(t.charAt(0)) >= 0) t = t.substring(1);
        while (t.length() > 1 && quotes.indexOf(t.charAt(t.length() - 1)) >= 0) t = t.substring(0, t.length() - 1);
        return t.trim();
    }

    public static String readCanon() {
        try {
            Path p = FabricLoader.getInstance().getConfigDir().resolve("rpmod_canon.txt");
            if (Files.exists(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                return s.length() > 6000 ? s.substring(0, 6000) : s;
            }
        } catch (Exception e) {
            RpHelperClient.LOGGER.warn("Не удалось прочитать rpmod_canon.txt", e);
        }
        return "";
    }
}
