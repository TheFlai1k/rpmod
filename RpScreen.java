package com.rpmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class RpScreen extends Screen {
    private static final int PANEL_W = 320;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int RED = 0xFFFF6B6B;
    private static final int GREEN = 0xFF7CFC9A;

    private RpConfig config = RpConfig.load();
    private ActionFormat format = ActionFormat.fromId(config.actionFormat);

    private TextFieldWidget hintField;
    private ButtonWidget generateBtn;
    private ButtonWidget redoBtn;
    private ButtonWidget sendBtn;
    private ButtonWidget formatBtn;

    private String result = "";
    private String lastHint = "";
    private String status = "";
    private int statusColor = GRAY;
    private boolean busy = false;

    public RpScreen() {
        super(Text.literal("RP-помощник"));
        if (config.loadError != null) {
            status = config.loadError;
            statusColor = RED;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        String keep = hintField != null ? hintField.getText() : "";
        int left = width / 2 - PANEL_W / 2;
        int top = height / 2 - 90;

        hintField = new TextFieldWidget(textRenderer, left, top + 24, PANEL_W, 20, Text.literal("Подсказка"));
        hintField.setMaxLength(200);
        hintField.setPlaceholder(Text.literal("Например: следит за торговцем"));
        hintField.setText(keep);
        addDrawableChild(hintField);
        setInitialFocus(hintField);

        int bw = (PANEL_W - 10) / 3;
        generateBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Сгенерировать"), b -> generate(false))
                .dimensions(left, top + 50, bw, 20).build());
        redoBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Переделать"), b -> generate(true))
                .dimensions(left + bw + 5, top + 50, bw, 20).build());
        sendBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Отправить в чат"), b -> sendToChat())
                .dimensions(left + (bw + 5) * 2, top + 50, bw, 20).build());

        formatBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Формат отправки: " + format.label), b -> {
            format = format.next();
            config.actionFormat = format.id;
            config.save();
            b.setMessage(Text.literal("Формат отправки: " + format.label));
        }).dimensions(left, top + 76, PANEL_W, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        generateBtn.active = !busy;
        redoBtn.active = !busy;
        sendBtn.active = !busy && !result.isBlank();
    }

    private void setStatus(String text, int color) {
        status = text;
        statusColor = color;
    }

    private void generate(boolean redo) {
        if (busy) return;
        String hint = hintField.getText().trim();
        if (redo && !lastHint.isEmpty()) hint = lastHint;
        if (hint.isEmpty()) {
            setStatus("Введите короткую подсказку, например: «следит за торговцем».", RED);
            return;
        }
        lastHint = hint;
        config = RpConfig.load(); // перечитываем файл: ключ можно менять без перезапуска
        if (config.loadError != null) {
            setStatus(config.loadError, RED);
            return;
        }

        String previous = redo ? result : "";
        String scene = config.contextAware ? Prompts.scene(client) : "";
        String system = Prompts.actionSystem(config);
        String user = Prompts.actionUser(hint, previous, scene);

        busy = true;
        setStatus("Думаю…", GRAY);
        updateButtons();

        MinecraftClient mc = client;
        GeminiClient.generate(config, system, user).whenComplete((text, error) -> mc.execute(() -> {
            busy = false;
            if (error != null) {
                Throwable root = (error.getCause() != null) ? error.getCause() : error;
                if (root instanceof GeminiClient.GeminiException) {
                    setStatus(root.getMessage(), RED);
                } else {
                    setStatus("Нет связи с Gemini. Проверьте интернет. (" + root.getClass().getSimpleName() + ")", RED);
                }
            } else {
                result = Prompts.cleanAction(text);
                setStatus("Готово. Можно отправить в чат или нажать «Переделать».", GREEN);
            }
            updateButtons();
        }));
    }

    private void sendToChat() {
        if (result.isBlank()) return;
        boolean ok = format.send(client.getNetworkHandler(), result);
        if (ok) setStatus("Отправлено: " + format.label, GREEN);
        else setStatus("Подождите секунду между отправками.", RED);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hintField.isFocused()) {
            generate(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int left = width / 2 - PANEL_W / 2;
        int top = height / 2 - 90;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("RP-помощник · Действие"), width / 2, top + 6, 0xFFFFFFFF);

        int y = top + 104;
        if (!result.isEmpty()) {
            for (OrderedText line : textRenderer.wrapLines(Text.literal(result), PANEL_W)) {
                context.drawTextWithShadow(textRenderer, line, left, y, 0xFFFFE9A8);
                y += 11;
            }
        }

        if (!status.isEmpty()) {
            int sy = top + 104 + 6 * 11 + 6;
            for (OrderedText line : textRenderer.wrapLines(Text.literal(status), PANEL_W)) {
                context.drawTextWithShadow(textRenderer, line, left, sy, statusColor);
                sy += 11;
            }
        }
    }
}
