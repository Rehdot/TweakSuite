package redot.tweaksuite.client;

import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import redot.tweaksuite.commons.Entrypoint;
import redot.tweaksuite.commons.SuiteClass;
import redot.tweaksuite.commons.SuiteThread;

import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUtility {

    private static final Pattern NAME_PATTERN = Pattern.compile("(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Constructor<?> jsfsConstructor;
    private static final Field jfoField;

    static {
        try {
            // private field
            jfoField = CachedCompiler.class.getDeclaredField("javaFileObjects");
            jfoField.setAccessible(true);

            // package-private class
            Class<?> clazz = Class.forName("net.openhft.compiler.JavaSourceFromString");
            jsfsConstructor = clazz.getDeclaredConstructor(String.class, String.class);
            jsfsConstructor.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void listenToSocket() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(49277)) {
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                        handleConnection(reader);
                    } catch (IOException e) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (IOException ignored) {}
        }, "TweakSuiteSocketListener").start();
    }

    private static void handleConnection(BufferedReader reader) throws IOException {
        List<String> classes = new LinkedList<>();
        StringBuilder currentClass = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.equals("---TWEAKSUITE-CLASS-END---")) {
                if (!currentClass.isEmpty()) {
                    classes.add(currentClass.toString());
                    currentClass = new StringBuilder();
                }
            } else if (line.equals("---TWEAKSUITE-KILL-SWITCH---")) {
                TweakSuiteClient.killProcesses();
                return;
            } else {
                if (!currentClass.isEmpty()) {
                    currentClass.append("\n");
                }
                currentClass.append(line);
            }
        }

        if (!currentClass.isEmpty()) {
            classes.add(currentClass.toString());
        }
        if (!classes.isEmpty()) {
            compileClasses(classes);
        }
    }

    public static void compileClasses(List<String> classDefs) {
        new Thread(() -> {
            List<SuiteClass> suiteClasses = createSuiteClasses(classDefs);
            compileAndLoadClasses(suiteClasses);
            runEntrypoints(suiteClasses);
        }, "TweakSuiteCompiler").start();
    }

    private static List<SuiteClass> createSuiteClasses(List<String> classDefs) {
        List<SuiteClass> suiteClasses = new ArrayList<>();

        for (String classDef : classDefs) {
            SuiteClass suiteClass = new SuiteClass();
            String className = extractClassName(classDef);

            suiteClass.setClassDef(classDef);
            suiteClass.setClassName(className);
            suiteClasses.add(suiteClass);
        }

        return suiteClasses;
    }

    private static void compileAndLoadClasses(List<SuiteClass> classes) {
        CachedCompiler compiler = new CachedCompiler(null, null);
        ClassLoader loader = new SandboxedClassLoader();
        ClientWriter writer = new ClientWriter(classes);

        populateCompilerJavaFileObjects(classes, compiler);

        if (!classes.isEmpty()) {
            SuiteClass leadClass = classes.get(0);
            loadLeadClass(leadClass, compiler, loader, writer);
        }

        for (SuiteClass suiteClass : classes) {
            Class<?> loadedClass = loadClass(loader, suiteClass.getClassName());
            suiteClass.setLiteralClass(loadedClass);

            if (loadedClass == null) {
                getLogger().warn("Class '{}' was null during compilation.", suiteClass.getClassName());
            }
        }
    }

    private static Class<?> loadClass(ClassLoader loader, String className) {
        try {
            return loader.loadClass(className);
        } catch (Exception e) {
            getLogger().error("Failed to load class '{}'", className, e);
            return null;
        }
    }

    private static void loadLeadClass(SuiteClass leadClass, CachedCompiler compiler, ClassLoader loader, ClientWriter writer) {
        try {
            compiler.loadFromJava(loader, leadClass.getClassName(), leadClass.getClassDef(), writer);
        } catch (Exception e) {
            getLogger().error("Failed to load lead class '{}'", leadClass.getClassName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void populateCompilerJavaFileObjects(List<SuiteClass> classes, CachedCompiler compiler) {
        try {
            var javaFileObjects = (ConcurrentMap<String, JavaFileObject>) jfoField.get(compiler);
            fillJavaFileObjectsMap(classes, javaFileObjects);
        } catch (IllegalAccessException e) {
            getLogger().error("Failed reflectively getting JavaFileObjects", e);
        }
    }

    private static void fillJavaFileObjectsMap(List<SuiteClass> classes, ConcurrentMap<String, JavaFileObject> javaFileObjects) {
        for (SuiteClass suiteClass : classes) {
            try {
                String className = suiteClass.getClassName();
                JavaFileObject jfo = (JavaFileObject) jsfsConstructor.newInstance(className, suiteClass.getClassDef());
                javaFileObjects.put(className, jfo);
            } catch (Exception e) {
                getLogger().error("Failed to create JavaFileObject for class '{}'", suiteClass.getClassName(), e);
            }
        }
    }

    private static void runEntrypoints(List<SuiteClass> classes) {
        for (SuiteClass suiteClass : classes) {
            if (suiteClass.getLiteralClass() == null) {
                getLogger().warn("Suite class '{}' was null.", suiteClass.getClassName());
                continue;
            }

            for (Method method : suiteClass.getLiteralClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Entrypoint.class)) {
                    SuiteThread thread = createInvokerThread(method);
                    TweakSuiteClient.getThreadRegistry().add(thread);
                    thread.start();
                }
            }
        }
    }

    @NotNull
    private static SuiteThread createInvokerThread(Method method) {
        AtomicReference<SuiteThread> threadReference = new AtomicReference<>();
        SuiteThread thread = new SuiteThread(() -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                getLogger().error("Failed executing Entrypoint: {}", e.getMessage(), e);
            }
            TweakSuiteClient.getThreadRegistry().remove(threadReference.get());
        }, "TweakSuiteInvoker");
        threadReference.set(thread);
        return thread;
    }

    public static String extractClassName(String classDef) {
        Matcher matcher = NAME_PATTERN.matcher(classDef);
        if (matcher.find()) {
            return matcher.group(1);
        }
        getLogger().warn("Could not extract class name from class definition.");
        return null;
    }

    private static Logger getLogger() {
        return TweakSuiteClient.getLogger();
    }
}