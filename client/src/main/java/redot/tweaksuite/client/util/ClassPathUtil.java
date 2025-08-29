package redot.tweaksuite.client.util;

import net.openhft.compiler.CompilerUtils;
import redot.tweaksuite.client.TweakSuiteClient;

import java.net.URISyntaxException;
import java.net.URL;

import static redot.tweaksuite.client.TweakSuiteClient.LOGGER;

/// Utility class to handle all class pathing needs
public class ClassPathUtil {

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

    public static <T> URL getURLFromObject(T object) throws URISyntaxException {
        return getURLFromClass(object.getClass());
    }

    public static URL getURLFromClass(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    public static <T> void addClassPathFromObject(T object) {
        try {
            URL url = getURLFromObject(object);
            addClassPathReference(url);
        } catch (URISyntaxException exception) {
            LOGGER.error(exception.getMessage());
        }
    }

    public static void addClassPathReference(URL url) {
        String jarPath = getFormattedPath(url);
        CompilerUtils.addClassPath(jarPath);
        LOGGER.info("Added {} to class path.", jarPath);
    }

    public static void addClassPathFromClass(Class<?> clazz) {
        try {
            URL url = getURLFromClass(clazz);
            addClassPathReference(url);
        } catch (Exception e) {
            LOGGER.error("Failed to add classpath from class {}: {}", clazz.getName(), e.getMessage());
        }
    }

    public static void addClassPathFromFQCN(String className) {
        try {
            Class<?> clazz = TweakSuiteClient.getBaseClassLoader().loadClass(className);
            addClassPathFromClass(clazz);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not find class {} to add to classpath", className);
        } catch (Exception e) {
            LOGGER.error("Failed to add classpath from class name {}: {}", className, e.getMessage());
        }
    }

}
