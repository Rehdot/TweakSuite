package redot.tweaksuite.client.util;

import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import redot.tweaksuite.client.model.ClientWriter;
import redot.tweaksuite.client.model.PermRegistry;
import redot.tweaksuite.client.model.SandboxedClassLoader;
import redot.tweaksuite.client.TweakSuiteClient;
import redot.tweaksuite.commons.Constants;
import redot.tweaksuite.commons.Entrypoint;
import redot.tweaksuite.commons.model.SuiteClass;
import redot.tweaksuite.commons.model.SuiteThread;
import redot.tweaksuite.commons.model.ThreadRegistry;

import javax.tools.JavaFileObject;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static redot.tweaksuite.client.TweakSuiteClient.LOGGER;

/// Utility class to handle all compilation needs
public class CompileUtil {

    private static final Constructor<?> jsfsConstructor;
    private static final Field jfoField;
    private static final File PERM_CLASS_DIR;
    private static final CachedCompiler PERM_COMPILER;

    static {
        try {
            PERM_CLASS_DIR = File.createTempFile("tweaksuite-perm", "");
            if (PERM_CLASS_DIR.exists()) PERM_CLASS_DIR.delete();
            PERM_CLASS_DIR.mkdirs();
            PERM_CLASS_DIR.deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp perm class directory", e);
        }
        try {
            // private field
            jfoField = CachedCompiler.class.getDeclaredField("javaFileObjects");
            jfoField.setAccessible(true);

            // package-private class
            Class<?> clazz = Class.forName("net.openhft.compiler.JavaSourceFromString");
            jsfsConstructor = clazz.getDeclaredConstructor(String.class, String.class);
            jsfsConstructor.setAccessible(true);

            PERM_COMPILER = new CachedCompiler(null, PERM_CLASS_DIR, List.of("-g", "-nowarn",
                    "-classpath", System.getProperty("java.class.path") + File.pathSeparator + PERM_CLASS_DIR.getAbsolutePath()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void compileClasses(List<String> tempClassDefs, List<String> permClassDefs) {
        new Thread(() -> {
            List<SuiteClass> tempSuiteClasses = createSuiteClasses(tempClassDefs);
            List<SuiteClass> permSuiteClasses = createSuiteClasses(permClassDefs);
            ClientWriter writer = new ClientWriter(tempClassDefs, permClassDefs);

            compileAndLoadPermClasses(permSuiteClasses, writer);
            compileAndLoadTempClasses(tempSuiteClasses, writer);

            runEntrypoints(permSuiteClasses);
            runEntrypoints(tempSuiteClasses);
        }, "TweakSuiteCompiler").start();
    }

    private static List<SuiteClass> createSuiteClasses(List<String> classDefs) {
        List<SuiteClass> suiteClasses = new ArrayList<>();
        for (String classDef : classDefs) {
            SuiteClass suiteClass = new SuiteClass();
            suiteClass.setClassDef(classDef);
            suiteClass.setClassName(Constants.extractClassName(classDef));
            suiteClasses.add(suiteClass);
        }
        return suiteClasses;
    }

    private static void compileAndLoadPermClasses(List<SuiteClass> classes, ClientWriter writer) {
        CachedCompiler compiler = PERM_COMPILER;
        ClassLoader loader = TweakSuiteClient.getBaseClassLoader();

        classes.removeIf(suiteClass -> {
            return PermRegistry.hasClass(suiteClass.getClassName());
        });

        populateAndLoadClasses(classes, compiler, loader, writer);

        for (SuiteClass suiteClass : classes) {
            PermRegistry.addClass(suiteClass.getClassName());
        }
    }

    private static void compileAndLoadTempClasses(List<SuiteClass> classes, ClientWriter writer) {
        CachedCompiler compiler = createTempCompiler();
        ClassLoader loader = new SandboxedClassLoader();

        populateAndLoadClasses(classes, compiler, loader, writer);
    }

    private static void populateAndLoadClasses(List<SuiteClass> classes, CachedCompiler compiler, ClassLoader loader, ClientWriter writer) {
        populateCompilerJavaFileObjects(classes, compiler);

        if (!classes.isEmpty()) {
            loadLeadClass(classes.get(0), compiler, loader, writer);
        }

        for (SuiteClass suiteClass : classes) {
            Class<?> loadedClass = loadClass(loader, suiteClass.getClassName());
            suiteClass.setLiteralClass(loadedClass);
            if (loadedClass == null) {
                LOGGER.warn("Class '{}' was null during compilation.", suiteClass.getClassName());
            }
        }
    }

    private static void loadLeadClass(SuiteClass leadClass, CachedCompiler compiler, ClassLoader loader, ClientWriter writer) {
        try {
            compiler.loadFromJava(loader, leadClass.getClassName(), leadClass.getClassDef(), writer);
        } catch (Exception e) {
            LOGGER.error("Failed to load lead class '{}'", leadClass.getClassName(), e);
        }
    }

    private static Class<?> loadClass(ClassLoader loader, String className) {
        try {
            return loader.loadClass(className);
        } catch (Exception e) {
            LOGGER.error("Failed to load class '{}'", className, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void populateCompilerJavaFileObjects(List<SuiteClass> classes, CachedCompiler compiler) {
        try {
            var javaFileObjects = (ConcurrentMap<String, JavaFileObject>) jfoField.get(compiler);
            for (SuiteClass suiteClass : classes) {
                if (PermRegistry.hasClass(suiteClass.getClassName())) continue;
                JavaFileObject jfo = (JavaFileObject) jsfsConstructor.newInstance(suiteClass.getClassName(), suiteClass.getClassDef());
                javaFileObjects.put(suiteClass.getClassName(), jfo);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to populate JavaFileObjects", e);
        }
    }

    private static void runEntrypoints(List<SuiteClass> classes) {
        for (SuiteClass suiteClass : classes) {
            for (Method method : suiteClass.getLiteralClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Entrypoint.class)) {
                    SuiteThread thread = createInvokerThread(method);
                    ThreadRegistry.REGISTRY.add(thread);
                    thread.start();
                }
            }
        }
    }

    @NotNull
    private static CachedCompiler createTempCompiler() {
        return new CachedCompiler(null, null, List.of("-g", "-nowarn",
                "-classpath", System.getProperty("java.class.path") + File.pathSeparator + PERM_CLASS_DIR.getAbsolutePath()));
    }

    @NotNull
    private static SuiteThread createInvokerThread(Method method) {
        AtomicReference<SuiteThread> threadReference = new AtomicReference<>();
        SuiteThread thread = new SuiteThread(() -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                LOGGER.error("Failed executing Entrypoint: {}", e.getMessage(), e);
            }
            ThreadRegistry.REGISTRY.remove(threadReference.get());
        }, "TweakSuiteInvoker");
        threadReference.set(thread);
        return thread;
    }
}
