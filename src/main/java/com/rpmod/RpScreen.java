package com.rpmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public class RpScreen extends Screen {

    private EditBox input;
    private String result = "";
    private String status = "";
    private boolean busy = false;

    public RpScreen() {
        super(Component.literal("РП-помощник"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        input = new EditBox(this.font, cx - 150, 40, 300, 20, Component.literal("Подсказка"));
        input.setMaxLength(200);
        input.setHint(Component.literal("Что делает персонаж? Например: следит за торговцем"));
        this.addRenderableWidget(input);
        this.setInitialFocus(input);

        this.addRenderableWidget(Button.builder(Component.literal("Сгенерировать"), b -> generate())
                .bounds(cx - 150, 70, 145, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Отправить в чат"), b -> send())
                .bounds(cx + 5, 70, 145, 20).build());
    }

    private void generate() {
        String hint = input.getValue().trim();
        if (hint.isEmpty() || busy) return;
        busy = true;
        status = "Думаю...";
        RpModClient.generate(hint).whenComplete((text, error) ->
                Minecraft.getInstance().execute(() -> {
                    busy = false;
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        status = "Ошибка: " + cause.getMessage();
                        result = "";
                    } else {
                        status = "";
                        result = text;
                    }
                }));
    }

    private void send() {
        if (result.isEmpty()) return;
        RpModClient.sendToChat(Minecraft.getInstance(), result);
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && input.isFocused()) {
            generate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, 15, 0xFFFFFFFF);

        int y = 105;
        if (!status.isEmpty()) {
            for (FormattedCharSequence line : this.font.split(FormattedText.of(status), 300)) {
                graphics.drawString(this.font, line, cx - 150, y, 0xFFFFAA00);
                y += 10;
            }
        }
        for (FormattedCharSequence line : this.font.split(FormattedText.of(result), 300)) {
            graphics.drawString(this.font, line, cx - 150, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
