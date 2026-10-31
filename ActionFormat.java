package com.rpmod;

import net.minecraft.client.network.ClientPlayNetworkHandler;

public enum ActionFormat {
    PLAIN("plain", "Обычный текст", null),
    ASTERISKS("asterisks", "*Звёздочки*", null),
    ME("me", "/me", "me"),
    DTRY("dtry", "/dtry", "dtry"),
    GME("gme", "/gme", "gme");

    public static final int CHAT_LIMIT = 256;
    private static final long MIN_PAUSE_MS = 1500;
    private static long lastSendMs = 0;

    public final String id;
    public final String label;
    private final String command; // без слэша; null для обычного чата

    ActionFormat(String id, String label, String command) {
        this.id = id;
        this.label = label;
        this.command = command;
    }

    public static ActionFormat fromId(String id) {
        if (id != null) {
            for (ActionFormat f : values()) {
                if (f.id.equalsIgnoreCase(id.trim())) return f;
            }
        }
        return ME;
    }

    public ActionFormat next() {
        ActionFormat[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** Возвращает false, если не отправили (пауза или пустой текст). */
    public boolean send(ClientPlayNetworkHandler handler, String text) {
        if (handler == null || text == null || text.isBlank()) return false;
        long now = System.currentTimeMillis();
        if (now - lastSendMs < MIN_PAUSE_MS) return false;
        text = text.trim();

        if (command != null) {
            int max = CHAT_LIMIT - command.length() - 1;
            if (text.length() > max) text = text.substring(0, max).trim();
            handler.sendChatCommand(command + " " + text);
        } else if (this == ASTERISKS) {
            int max = CHAT_LIMIT - 2;
            if (text.length() > max) text = text.substring(0, max).trim();
            handler.sendChatMessage("*" + text + "*");
        } else {
            if (text.length() > CHAT_LIMIT) text = text.substring(0, CHAT_LIMIT).trim();
            handler.sendChatMessage(text);
        }
        lastSendMs = now;
        return true;
    }
}
