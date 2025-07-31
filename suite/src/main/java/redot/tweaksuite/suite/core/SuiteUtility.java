package redot.tweaksuite.suite.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

public class SuiteUtility {

    /**
     * Decompiles a jar file and returns a list of class source code strings
     */
    public static List<String> decompileJar(String jarPath) {
        List<String> classDefinitions = new LinkedList<>();

        try {
            Map<String, Object> options = Map.of(
                    IFernflowerPreferences.REMOVE_SYNTHETIC, "false",
                    IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "true",
                    IFernflowerPreferences.LOG_LEVEL, "TRACE"
            );

            Fernflower fernflower = getFernflower(jarPath, classDefinitions, options);
            fernflower.decompileContext();
        } catch (Exception e) {
            System.err.println("De-compilation failure:\n" + e.getMessage());
            e.printStackTrace();
        }

        return classDefinitions;
    }

    @NotNull
    private static Fernflower getFernflower(String jarPath, List<String> classDefinitions, Map<String, Object> options) {
        IResultSaver resultSaver = new IResultSaver() {
            @Override
            public void saveClassEntry(String s, String s1, String s2, String s3, String content) {
                classDefinitions.add(content);
            }

            @Override public void saveFolder(String path) {}
            @Override public void copyFile(String source, String path, String entryName) {}
            @Override public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {}
            @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
            @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
            @Override public void copyEntry(String source, String path, String archiveName, String entry) {}
            @Override public void closeArchive(String path, String archiveName) {}
        };

        Fernflower fernflower = new Fernflower(resultSaver, options, getFFLogger());
        fernflower.addSource(new File(jarPath));
        return fernflower;
    }

    public static void sendClassesOverSocket(List<String> classes) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 49277);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            for (String classDef : classes) {
                classDef = classDef.replaceFirst("(?m)^package\\s+[\\w.]+;\\s*", "// <3\n")
                        .replace("import redot.tweaksuite.suite.sandbox", "// import redot.tweaksuite.suite.sandbox");
                writer.write(classDef);
                writer.newLine();
                writer.write("---TWEAKSUITE-CLASS-END---");
                writer.newLine();
            }

            writer.flush();
        }
    }

    @NotNull
    private static IFernflowerLogger getFFLogger() {
        return new IFernflowerLogger() {
            @Override
            public void writeMessage(String message, Severity severity) {
                System.out.println("[" + severity + "] " + message);
            }

            @Override
            public void writeMessage(String message, Severity severity, Throwable t) {
                System.out.println("[" + severity + "] " + message);
                if (t != null) {
                    t.printStackTrace();
                }
            }

            @Override public void setSeverity(Severity severity) {}
        };
    }

}