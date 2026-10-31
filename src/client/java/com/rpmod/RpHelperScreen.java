package com.rpmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class RpHelperScreen extends Screen {

    private TextFieldWidget input;
    private String result = "";
    private String status = "";
    private boolean busy = false;

    public RpHelperScreen() {
        super(Text.literal("РП-помощник"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        input = new TextFieldWidget(this.textRenderer, cx - 150, 40, 300, 20, Text.literal("Подсказка"));
        input.setMaxLength(200);
        input.setPlaceholder(Text.literal("Что делает персонаж? Например: следит за торговцем"));
        this.addDrawableChild(input);
        this.setInitialFocus(input);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сгенерировать"), b -> generate())
                .dimensions(cx - 150, 70, 145, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Отправить в чат"), b -> send())
                .dimensions(cx + 5, 70, 145, 20).build());
    }

    private void generate() {
        String hint = input.getText().trim();
        if (hint.isEmpty() || busy) return;
        busy = true;
        status = "Думаю...";
        RpHelperClient.generate(hint).whenComplete((text, error) ->
                MinecraftClient.getInstance().execute(() -> {
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
        RpHelperClient.sendToChat(MinecraftClient.getInstance(), result);
        this.close();
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 15, 0xFFFFFFFF);

        int y = 105;
        if (!status.isEmpty()) {
            for (OrderedText line : this.textRenderer.wrapLines(StringVisitable.plain(status), 300)) {
                context.drawTextWithShadow(this.textRenderer, line, cx - 150, y, 0xFFFFAA00);
                y += 10;
            }
        }
        for (OrderedText line : this.textRenderer.wrapLines(StringVisitable.plain(result), 300)) {
            context.drawTextWithShadow(this.textRenderer, line, cx - 150, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
