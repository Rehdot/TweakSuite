package redot.tweaksuite.client;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import redot.tweaksuite.client.util.ClassPathUtil;
import redot.tweaksuite.client.util.CompileUtil;
import redot.tweaksuite.commons.model.SuiteClass;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A PrintWriter to aid in automatically adding sources to the class path and re-compiling
public class ClientWriter extends PrintWriter {

    private static final Pattern ERROR_PATTERN = Pattern.compile("error: (?:package|cannot access) ([a-zA-Z_][\\w$]*(?:\\.[\\w$]+)*)");
    private static final Set<String> COMPILED_FQCNS = Sets.newHashSet();
    private static final PrintWriter DEFAULT_WRITER;

    private final List<String> classes;

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

    public ClientWriter(List<SuiteClass> classes) {
        this(classes.stream().map(SuiteClass::getClassDef).toList(), false);
    }

    public ClientWriter(List<String> classes, boolean ignored) {
        super(DEFAULT_WRITER);
        this.classes = classes;
    }

    @Override
    public void write(@NotNull String s) {
        super.write(s);
        if (s.contains("error:")) {
            String fqcn = extractFQCN(s);
            if (fqcn == null || fqcn.isEmpty()) return;
            ClassPathUtil.addClassPathFromFQCN(fqcn);
            CompileUtil.compileClasses(this.classes);
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
