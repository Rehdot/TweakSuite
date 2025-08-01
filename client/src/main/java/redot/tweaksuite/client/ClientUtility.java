package redot.tweaksuite.client;

import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import redot.tweaksuite.commons.Entrypoint;
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

    public static void compileClasses(List<String> classes) {
        new Thread(() -> {
            CachedCompiler compiler = new CachedCompiler(null, null);
            ClassLoader classLoader = new SandboxedClassLoader();
            List<Class<?>> classList = new ArrayList<>();

            TweakSuiteClient.getLogger().info("Starting compilation of {} class(es).", classes.size());

            try {
                for (String classDef : classes) { // for circular dependencies
                    String className = extractClassName(classDef);
                    var javaFileObjects = (ConcurrentMap<String, JavaFileObject>) jfoField.get(compiler);
                    javaFileObjects.put(className, (JavaFileObject) jsfsConstructor.newInstance(className, classDef));
                }

                ClientWriter clientWriter = new ClientWriter(classes);
                String leadClassName = extractClassName(classes.get(0));
                Class<?> leadClass = compiler.loadFromJava(classLoader, leadClassName, classes.get(0), clientWriter);
                TweakSuiteClient.getLogger().info("Loaded lead class: {}", leadClassName);

                for (String classDef : classes) {
                    String className = extractClassName(classDef);
                    Class<?> clazz = classLoader.loadClass(className);
                    TweakSuiteClient.getLogger().info("Loaded class: {}", className);
                    classList.add(clazz);
                }

            } catch (Exception e) {
                TweakSuiteClient.getLogger().error("Compilation failed: {}", e.toString());
            }

            runEntrypoint(classList);
        }, "TweakSuiteCompiler").start();
    }


    private static void runEntrypoint(List<Class<?>> cList) {
        for (Class<?> clazz : cList) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Entrypoint.class)) {
                    SuiteThread thread = getInvokerThread(method);
                    TweakSuiteClient.getThreadRegistry().add(thread);
                    thread.start();
                }
            }
        }
    }

    @NotNull
    private static SuiteThread getInvokerThread(Method method) {
        AtomicReference<SuiteThread> threadReference = new AtomicReference<>();
        SuiteThread thread = new SuiteThread(() -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                TweakSuiteClient.getLogger().error("Failed executing Entrypoint:\n{}", e.getMessage());
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
        TweakSuiteClient.getLogger().warn("Matcher could not find class name.");
        return null;
    }

}