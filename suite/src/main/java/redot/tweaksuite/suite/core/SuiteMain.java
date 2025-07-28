package redot.tweaksuite.suite.core;

import java.io.IOException;
import java.util.List;

public class SuiteMain {

    public static void main(String[] args) {
        String remappedJarPath = System.getProperty("remapped.jar.path");

        if (remappedJarPath == null) {
            System.err.println("Remapped jar path not provided!");
            return;
        }

        System.out.println("Decompiling jar...");

        List<String> classes = SuiteUtility.decompileJar(remappedJarPath);
        System.out.println("Decompiled " + classes.size() + " class(es).");

        for (String classDef : classes) {
            System.out.println(classDef);
        }

        try {
            SuiteUtility.sendClassesOverSocket(classes);
            System.out.println("Sent classes through Socket.");
        } catch (IOException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
    }
}