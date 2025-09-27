package redot.tweaksuite.server.log;

import redot.tweaksuite.server.TweakSuite;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A PrintWriter to aid in automatically adding sources to the class path and re-compiling
public class SuiteWriter extends PrintWriter {

    private static final Pattern ERROR_PATTERN = Pattern.compile("error: (?:package|cannot access) ([a-zA-Z_][\\w$]*(?:\\.[\\w$]+)*)");
    private static final Set<String> COMPILED_FQCNS = new HashSet<>();
    private static final PrintWriter DEFAULT_WRITER;

    private final List<String> tempClasses, permClasses;
    private final TweakSuite tweakSuite;

    static {
        PrintWriter writer;
        try {
            Class<?> cachedCompilerClass = Class.forName("net.openhft.compiler.CachedCompiler");
            Field defaultWriterField = cachedCompilerClass.getDeclaredField("DEFAULT_WRITER");
            defaultWriterField.setAccessible(true);
            writer = (PrintWriter) defaultWriterField.get(null);
        } catch (Exception e) {
            writer = null;
        }
        DEFAULT_WRITER = writer;
    }

    public SuiteWriter(TweakSuite tweakSuite, List<String> tempClasses, List<String> permClasses) {
        super(DEFAULT_WRITER);
        this.tweakSuite = tweakSuite;
        this.tempClasses = tempClasses;
        this.permClasses = permClasses;
    }

    @Override
    public void write(String s) {
        super.write(s);
        if (s.contains("error:")) {
            String fqcn = extractFQCN(s);
            if (fqcn == null || fqcn.isEmpty()) return;
            this.tweakSuite.getClassPathWorker().addClassPathFromFQCN(fqcn);
            this.tweakSuite.getCompiler().compileClasses(this.tempClasses, this.permClasses);
        }
    }

    private static String extractFQCN(String s) {
        Matcher matcher = ERROR_PATTERN.matcher(s);

        if (matcher.find()) {
            String fqcn = matcher.group(1);
            if (COMPILED_FQCNS.contains(fqcn)) return null;
            COMPILED_FQCNS.add(fqcn);
            return fqcn;
        }

        return null;
    }

}
