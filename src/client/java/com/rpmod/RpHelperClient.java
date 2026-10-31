package com.rpmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpHelperClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("rpmod");

    public static KeyBinding openKey;
    public static KeyBinding wheelKey;
    public static KeyBinding wheelSwitchKey;

    @Override
    public void onInitializeClient() {
        RpConfig.load(); // создаст config/rpmod.json при первом запуске

        String category = "key.categories.rpmod";
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.rpmod.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        wheelKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.rpmod.wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
        wheelSwitchKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.rpmod.wheel_switch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new RpScreen());
            }
            while (wheelKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new GestureWheelScreen());
            }
            while (wheelSwitchKey.wasPressed()) {
                // нажатия вне колеса просто сбрасываем
            }
        });
    }
}
