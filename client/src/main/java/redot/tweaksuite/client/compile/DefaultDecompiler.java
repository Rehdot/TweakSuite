package redot.tweaksuite.client.compile;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

public class DefaultDecompiler implements SuiteDecompiler {

    @Override
    public List<String> decompileJar(String jarPath) {
        List<String> classDefinitions = new LinkedList<>();

        try {
            Map<String, Object> options = Map.of(
                    IFernflowerPreferences.REMOVE_SYNTHETIC, "false",
                    IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "true",
                    IFernflowerPreferences.LOG_LEVEL, "TRACE"
            );

            Fernflower fernflower = this.getFernflower(jarPath, classDefinitions, options);
            fernflower.decompileContext();
        } catch (Exception e) {
            System.err.println("De-compilation failure:\n" + e.getMessage());
            e.printStackTrace();
        }

        return classDefinitions;
    }

    private Fernflower getFernflower(String jarPath, List<String> classDefinitions, Map<String, Object> options) {
        IResultSaver resultSaver = new ResultSaver() {
            @Override
            public void saveClassEntry(String s, String s1, String s2, String s3, String content) {
                System.out.println(content);
                classDefinitions.add(content);
            }
        };
        Fernflower fernflower = new Fernflower(resultSaver, options, new SuiteFFLogger());

        fernflower.addSource(new File(jarPath));
        return fernflower;
    }

    private static class SuiteFFLogger extends IFernflowerLogger {
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
    }

    private static abstract class ResultSaver implements IResultSaver {
        @Override public void saveFolder(String path) {}
        @Override public void copyFile(String source, String path, String entryName) {}
        @Override public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {}
        @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
        @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
        @Override public void copyEntry(String source, String path, String archiveName, String entry) {}
        @Override public void closeArchive(String path, String archiveName) {}
    }

}
