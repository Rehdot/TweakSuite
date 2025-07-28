package redot.tweaksuite.client;

import net.openhft.compiler.CachedCompiler;
import org.jetbrains.annotations.NotNull;
import redot.tweaksuite.commons.Entrypoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUtility {

    private static final Pattern NAME_PATTERN = Pattern.compile("(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    public static void listenToSocket() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(49277)) {
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                        handleConnection(reader);
                    }
                    catch (IOException e) {
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

    private static void compileClasses(List<String> classes) {
        new Thread(() -> {
            CachedCompiler compiler = new CachedCompiler(null, null);
            ClassLoader classLoader = new SandboxedClassLoader();
            List<Class<?>> cList = new LinkedList<>();

            TweakSuiteClient.getLogger().info("Starting compilation of {} class(es).", classes.size());

            for (String classDef : classes) {
                String className = extractClassName(classDef);
                try {
                    Class<?> clazz = compiler.loadFromJava(classLoader, className, classDef);
                    TweakSuiteClient.getLogger().info("Loaded class: {}", className);
                    cList.add(clazz);
                } catch (Exception e) {
                    TweakSuiteClient.getLogger().error("Failed compiling class:\n{}\n\nError:\n{}", classDef, e.getMessage());
                }
            }

            runEntrypoint(cList);
        }, "TweakSuiteCompiler").start();

    }

    private static void runEntrypoint(List<Class<?>> cList) {
        for (Class<?> clazz : cList) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Entrypoint.class)) {
                    Thread thread = getInvokerThread(method);
                    TweakSuiteClient.getThreadRegistry().add(thread);
                    thread.start();
                }
            }
        }
    }

    @NotNull
    private static Thread getInvokerThread(Method method) {
        AtomicReference<Thread> threadReference = new AtomicReference<>();
        Thread thread = new Thread(() -> {
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
            String find = matcher.group(1);
            TweakSuiteClient.getLogger().info("Matcher found class name: {}", find);
            return find;
        }
        TweakSuiteClient.getLogger().warn("Matcher could not find class name.");
        return null;
    }

}