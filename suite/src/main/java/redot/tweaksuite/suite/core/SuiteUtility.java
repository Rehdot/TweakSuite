package redot.tweaksuite.suite.core;

import com.google.common.collect.Lists;
import org.benf.cfr.reader.api.CfrDriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SuiteUtility {

    /**
     * Decompiles a jar file and returns a list of class source code strings
     */
    public static List<String> decompileJar(String jarPath) {
        List<String> classDefinitions = Lists.newLinkedList();
        File tempDir = null;

        try {
            tempDir = Files.createTempDirectory("cfr-decompile").toFile();

            HashMap<String, String> options = new HashMap<>();
            options.put("outputdir", tempDir.getAbsolutePath());

            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .build();
            driver.analyse(Arrays.asList(jarPath));

            findJavaFiles(tempDir, classDefinitions);
        } catch (Exception e) {
            System.err.println("De-compilation failure:\n" + e.getMessage());
        } finally {
            if (tempDir != null) {
                deleteDir(tempDir);
            }
        }

        return classDefinitions;
    }

    private static void findJavaFiles(File dir, List<String> results) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findJavaFiles(file, results);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    results.add(content);
                } catch (Exception e) {
                    System.err.println("Failed to read " + file.getName());
                }
            }
        }
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }


    /**
     * Sends class definitions over a socket.
     */
    public static void sendClassesOverSocket(List<String> classes) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 49277);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            for (String classDef : classes) {
                classDef = classDef.replaceFirst("(?m)^package\\s+[\\w.]+;\\s*", "// <3\n");
                writer.write(classDef);
                writer.newLine();
                writer.write("---TWEAKSUITE-CLASS-END---");
                writer.newLine();
            }

            writer.flush();
        }
    }

}