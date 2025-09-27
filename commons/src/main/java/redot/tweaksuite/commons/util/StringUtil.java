package redot.tweaksuite.commons.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    public static String extractClassName(String classDef) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(classDef);
        if (matcher.find()) {
            return matcher.group(1);
        }
        System.err.println("Could not extract class name from class definition:\n" + classDef);
        return null;
    }

}
