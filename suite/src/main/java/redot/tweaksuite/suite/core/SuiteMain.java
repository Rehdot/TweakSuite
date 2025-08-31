package redot.tweaksuite.suite.core;

import org.jetbrains.annotations.NotNull;
import redot.tweaksuite.suite.core.util.ClassValidator;
import redot.tweaksuite.suite.core.util.DecompileUtil;
import redot.tweaksuite.suite.core.util.SocketUtil;

import java.io.IOException;
import java.util.List;

public class SuiteMain {

    public static void main(String[] args) {
        runPipeline();
    }

    private static void runPipeline() {
        String remappedJarPath = System.getProperty("remapped.jar.path");

        if (remappedJarPath == null) {
            System.err.println("Remapped jar path not provided!");
            return;
        }

        List<String> classes = decompileJar(remappedJarPath);
        ClassValidator.validateJar(remappedJarPath);
        sendClasses(classes);
    }

    private static void sendClasses(List<String> classes) {
        try {
            SocketUtil.sendClassesOverSocket(classes);
            System.out.println("Sent classes through Socket.");
        } catch (IOException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
    }

    @NotNull
    private static List<String> decompileJar(String remappedJarPath) {
        System.out.println("Decompiling jar...");

        List<String> classes = DecompileUtil.decompileJar(remappedJarPath);

        System.out.println("Decompiled " + classes.size() + " class(es).");
//        classes.forEach(System.out::println);

        return classes;
    }

}