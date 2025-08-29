package redot.tweaksuite.client;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redot.tweaksuite.client.util.ClassPathUtil;
import redot.tweaksuite.client.util.ConnectionUtil;
import redot.tweaksuite.commons.Entrypoint;
import redot.tweaksuite.commons.model.ThreadRegistry;

public class TweakSuiteClient implements ModInitializer {

    @Getter @Setter
    private static ClassLoader baseClassLoader = TweakSuiteClient.class.getClassLoader();
    public static final Logger LOGGER = LoggerFactory.getLogger("tweaksuite");

    @Override
    public void onInitialize() {
        updateClassPath();
        registerKeybinds();
        ConnectionUtil.listenToSocket();
    }

    private void registerKeybinds() {
        KeyBinding killKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("Kill Switch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "TweakSuite")
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (killKey.wasPressed()) {
                ThreadRegistry.killProcesses();
            }
        });
    }

    public static void updateClassPath() {
        ClassPathUtil.addClassPathFromClass(TweakSuiteClient.class);
        ClassPathUtil.addClassPathFromObject(MinecraftClient.getInstance());
        ClassPathUtil.addClassPathFromObject(FabricLoader.getInstance());
        ClassPathUtil.addClassPathFromClass(Entrypoint.class);
    }

}