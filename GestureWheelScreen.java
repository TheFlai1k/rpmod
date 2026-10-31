package com.rpmod;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class GestureWheelScreen extends Screen {
    private static int setIndex = 0; // помним выбранный набор между открытиями

    private final RpConfig config = RpConfig.load();
    private int selected = -1;

    public GestureWheelScreen() {
        super(Text.literal("Колесо жестов"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<RpConfig.Gesture> gestures() {
        return setIndex == 0 ? config.gestureSet1 : config.gestureSet2;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int code = KeyBindingHelper.getBoundKeyOf(RpHelperClient.wheelKey).getCode();
        boolean held = GLFW.glfwGetKey(mc.getWindow().getHandle(), code) == GLFW.GLFW_PRESS;
        if (!held) {
            confirm(mc);
        }
    }

    private void confirm(MinecraftClient mc) {
        List<RpConfig.Gesture> list = gestures();
        int idx = selected;
        mc.setScreen(null);
        if (idx < 0 || idx >= list.size() || mc.player == null) return;
        String text = list.get(idx).text;
        boolean ok = ActionFormat.fromId(config.actionFormat).send(mc.getNetworkHandler(), text);
        if (!ok) {
            mc.player.sendMessage(Text.literal("RP-помощник: подождите секунду между отправками."), true);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (RpHelperClient.wheelSwitchKey.matchesKey(keyCode, scanCode)) {
            setIndex = 1 - setIndex;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // Esc закрывает без отправки
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);

        int cx = width / 2;
        int cy = height / 2;
        List<RpConfig.Gesture> list = gestures();
        int n = Math.min(8, list.size());

        selected = -1;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        if (n > 0 && Math.sqrt(dx * dx + dy * dy) > 25) {
            double angle = Math.atan2(dx, -dy); // 0 = вверх, по часовой стрелке
            if (angle < 0) angle += 2 * Math.PI;
            selected = (int) Math.round(angle / (2 * Math.PI / n)) % n;
        }

        int rx = Math.min(width / 2 - 70, 170);
        int ry = Math.min(height / 2 - 40, 110);
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            int x = cx + (int) Math.round(Math.sin(a) * rx);
            int y = cy - (int) Math.round(Math.cos(a) * ry);
            String name = list.get(i).name;
            int w = textRenderer.getWidth(name) + 12;
            int x1 = x - w / 2;
            int y1 = y - 9;
            context.fill(x1, y1, x1 + w, y1 + 18, i == selected ? 0xFFB8860B : 0xCC202020);
            context.drawCenteredTextWithShadow(textRenderer, name, x, y - 4, 0xFFFFFFFF);
        }

        Text header = Text.literal("Набор " + (setIndex + 1) + "/2  ·  ")
                .append(RpHelperClient.wheelSwitchKey.getBoundKeyLocalizedText())
                .append(Text.literal(" — сменить"));
        context.drawCenteredTextWithShadow(textRenderer, header, cx, cy - 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Отпустите клавишу — отправить · Esc — отмена"), cx, cy + 2, 0xFFAAAAAA);

        if (selected >= 0 && selected < list.size()) {
            int y = height - 50;
            for (OrderedText line : textRenderer.wrapLines(Text.literal(list.get(selected).text), Math.min(width - 40, 360))) {
                context.drawCenteredTextWithShadow(textRenderer, line, cx, y, 0xFFFFE9A8);
                y += 11;
            }
        }
    }
}
