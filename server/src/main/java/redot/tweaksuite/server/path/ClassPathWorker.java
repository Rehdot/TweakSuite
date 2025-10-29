package redot.tweaksuite.server.path;

import lombok.RequiredArgsConstructor;
import net.openhft.compiler.CompilerUtils;
import redot.tweaksuite.commons.annotation.Entrypoint;
import redot.tweaksuite.server.TweakSuite;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClassPathWorker {

    private final TweakSuite tweakSuite;

    public void resolve(ClassResolver resolver) {
        resolver.includedClasses().forEach(this::addClassPathFromClass);
        resolver.includedObjects().forEach(this::addClassPathFromObject);
        this.addClassPathFromClass(Entrypoint.class);
    }

    public String getFormattedPath(URL url) {
        String jarPath = url.getPath().split("!")[0];
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.replaceFirst("file:", "");
        }
        if (!jarPath.endsWith("/")) {
            jarPath = jarPath + "/";
        }
        return jarPath;
    }

    public <T> URL getURLFromObject(T object) throws URISyntaxException {
        return getURLFromClass(object.getClass());
    }

    public URL getURLFromClass(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    public <T> void addClassPathFromObject(T object) {
        try {
            URL url = getURLFromObject(object);
            addClassPathReference(url);
        } catch (URISyntaxException exception) {
            this.tweakSuite.getLogger().error("Failed to add classpath: {}", exception.getMessage());
        }
    }

    public void addClassPathReference(URL url) {
        String jarPath = getFormattedPath(url);
        CompilerUtils.addClassPath(jarPath);
        this.tweakSuite.getLogger().info("Added {} to class path.", jarPath);
    }

    public void addClassPathFromClass(Class<?> clazz) {
        try {
            URL url = getURLFromClass(clazz);
            addClassPathReference(url);
        } catch (Exception e) {
            this.tweakSuite.getLogger().error("Failed to add classpath from class {}: {}", clazz.getName(), e.getMessage());
        }
    }

    public void addClassPathFromFQCN(String className) {
        try {
            Class<?> clazz = this.tweakSuite.getBaseClassLoader().loadClass(className);
            addClassPathFromClass(clazz);
        } catch (ClassNotFoundException e) {
            this.tweakSuite.getLogger().error("Could not find class {} to add to classpath", className);
        } catch (Exception e) {
            this.tweakSuite.getLogger().error("Failed to add classpath from class name {}: {}", className, e.getMessage());
        }
    }

}
