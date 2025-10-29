package redot.tweaksuite.server.log;

import java.util.regex.Matcher;

public interface SuiteLogger {

    void info(String s);
    void warn(String s);
    void error(String s);

    default void info(String s, String... args) {
        this.info(applyArgs(s, args));
    }

    default void warn(String s, String... args) {
        this.warn(applyArgs(s, args));
    }

    default void error(String s, String... args) {
        this.error(applyArgs(s, args));
    }

    default String applyArgs(String s, String... args) {
        if (s == null) return "";
        for (String arg : args) {
            if (!s.contains("{}")) return s;
            s = s.replaceFirst("\\{}", arg == null ? "null" : Matcher.quoteReplacement(arg));
        }
        return s;
    }
}
