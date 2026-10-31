package com.rpmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RpConfig {
    public static class Gesture {
        public String name = "";
        public String text = "";

        public Gesture() {}

        public Gesture(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    public String apiKey = "";
    public String model = "gemini-2.5-flash";
    public String characterName = "Мой персонаж";
    public String character = "Опишите здесь своего персонажа: кто он, откуда, характер, манера держаться.";
    public String actionFormat = "me";   // plain, asterisks, me, dtry, gme
    public String loreSource = "books";  // books или show (пригодится на этапе 2)
    public boolean contextAware = true;
    public List<Gesture> gestureSet1 = defaultSet1();
    public List<Gesture> gestureSet2 = defaultSet2();

    // не попадает в файл
    public transient String loadError = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("rpmod.json");
    }

    public static RpConfig load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                RpConfig c = GSON.fromJson(r, RpConfig.class);
                if (c != null) {
                    c.fixNulls();
                    return c;
                }
            } catch (Exception e) {
                RpHelperClient.LOGGER.warn("Не удалось прочитать rpmod.json", e);
                RpConfig broken = new RpConfig();
                broken.loadError = "Файл config/rpmod.json повреждён (скорее всего, потерялась запятая или кавычка). Исправьте его.";
                return broken;
            }
        }
        RpConfig c = new RpConfig();
        c.save();
        return c;
    }

    public void save() {
        if (loadError != null) return; // не затираем файл, который не удалось прочитать
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RpHelperClient.LOGGER.warn("Не удалось сохранить rpmod.json", e);
        }
    }

    private void fixNulls() {
        if (apiKey == null) apiKey = "";
        if (model == null || model.isBlank()) model = "gemini-2.5-flash";
        if (characterName == null) characterName = "";
        if (character == null) character = "";
        if (actionFormat == null) actionFormat = "me";
        if (loreSource == null) loreSource = "books";
        if (gestureSet1 == null || gestureSet1.isEmpty()) gestureSet1 = defaultSet1();
        if (gestureSet2 == null || gestureSet2.isEmpty()) gestureSet2 = defaultSet2();
    }

    private static List<Gesture> defaultSet1() {
        List<Gesture> l = new ArrayList<>();
        l.add(new Gesture("Поклон", "Склоняет голову в почтительном поклоне."));
        l.add(new Gesture("Преклонить колено", "Опускается на одно колено, не поднимая взгляда."));
        l.add(new Gesture("Кивок", "Коротко кивает, давая понять, что услышал."));
        l.add(new Gesture("Приветствие воина", "Прикладывает кулак к груди в знак уважения."));
        l.add(new Gesture("Нахмуриться", "Хмурит брови, обдумывая услышанное."));
        l.add(new Gesture("Прищуриться", "Прищуривается, внимательно изучая собеседника."));
        l.add(new Gesture("Усмешка", "Криво усмехается, не отводя взгляда."));
        l.add(new Gesture("Вздох", "Тяжело вздыхает и качает головой."));
        return l;
    }

    private static List<Gesture> defaultSet2() {
        List<Gesture> l = new ArrayList<>();
        l.add(new Gesture("Тишина", "Поднимает ладонь, призывая всех к молчанию."));
        l.add(new Gesture("Рука на эфесе", "Кладёт ладонь на эфес меча, но не обнажает его."));
        l.add(new Gesture("Тост", "Поднимает кубок, обводя взглядом собравшихся."));
        l.add(new Gesture("Шёпот", "Наклоняется ближе и понижает голос до шёпота."));
        l.add(new Gesture("Оглядеться", "Бросает быстрый взгляд через плечо."));
        l.add(new Gesture("Отвернуться", "Демонстративно отворачивается, не желая продолжать разговор."));
        l.add(new Gesture("Скрестить руки", "Скрещивает руки на груди и молча ждёт ответа."));
        l.add(new Gesture("Палец к губам", "Прикладывает палец к губам, прося молчать."));
        return l;
    }
}
