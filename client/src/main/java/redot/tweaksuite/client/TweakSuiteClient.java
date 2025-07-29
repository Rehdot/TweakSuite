package redot.tweaksuite.client;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.openhft.compiler.CompilerUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redot.tweaksuite.commons.Entrypoint;
import redot.tweaksuite.commons.SuiteThread;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class TweakSuiteClient implements ModInitializer {

    @Getter
    private static final Logger logger = LoggerFactory.getLogger("tweaksuite");
    @Getter
    private static final List<SuiteThread> threadRegistry = Lists.newLinkedList();

    @Override
    public void onInitialize() {
        updateClassPath();
        registerKeybinds();
        ClientUtility.listenToSocket();
    }

    private void registerKeybinds() {
        KeyBinding killKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("Kill Switch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "TweakSuite")
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (killKey.wasPressed()) {
                killProcesses();
            }
        });
    }

    public static void killProcesses() {
        killProcessesSafe();
        killProcessesUnsafe();
    }

    /// Disallows the threads from running. This approach only works
    /// if the user checks the SuiteThread's permit in their code.
    public static void killProcessesSafe() {
        threadRegistry.forEach(thread -> thread.setPermitted(false));
    }

    /// Tries to kill the threads at all costs
    public static void killProcessesUnsafe() {
        try {
            threadRegistry.forEach(Thread::stop);
            threadRegistry.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateClassPath() {
        addClassPathFromClass(TweakSuiteClient.class);
        addClassPathFromObject(MinecraftClient.getInstance());
        addClassPathFromObject(FabricLoader.getInstance());
        addClassPathFromClass(Entrypoint.class);
    }

    public static String getFormattedPath(URL url) {
        String jarPath = url.getPath().split("!")[0];
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.replaceFirst("file:", "");
        }
        if (!jarPath.endsWith("/")) {
            jarPath = jarPath + "/";
        }
        return jarPath;
    }

    private static <T> URL getURLFromObject(T object) throws URISyntaxException {
        return object.getClass().getProtectionDomain().getCodeSource().getLocation();
    }

    private static URL getURLFromClass(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    public static <T> void addClassPathFromObject(T object) {
        try {
            URL url = getURLFromObject(object);
            addClassPathReference(url);
        } catch (URISyntaxException exception) {
            logger.error(exception.getMessage());
        }
    }

    public static void addClassPathReference(URL url) {
        String jarPath = getFormattedPath(url);
        CompilerUtils.addClassPath(jarPath);
        logger.info("Added {} to class path.", jarPath);
    }

    public static void addClassPathFromClass(Class<?> clazz) {
        try {
            URL url = getURLFromClass(clazz);
            addClassPathReference(url);
        } catch (Exception e) {
            logger.error("Failed to add classpath from class {}: {}", clazz.getName(), e.getMessage());
        }
    }

    public static void addClassPathFromFQCN(String className) {
        try {
            // Try to load the class first
            Class<?> clazz = TweakSuiteClient.class.getClassLoader().loadClass(className);
            addClassPathFromClass(clazz);
        } catch (ClassNotFoundException e) {
            logger.error("Could not find class {} to add to classpath", className);
        } catch (Exception e) {
            logger.error("Failed to add classpath from class name {}: {}", className, e.getMessage());
        }
    }

}