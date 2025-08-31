package redot.tweaksuite.commons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {

    public static final String CLASS_END_STRING = "---TWEAKSUITE-CLASS-END---";
    public static final String PERM_CLASS_END_STRING = "---TWEAKSUITE-PERM-CLASS-END---";
    public static final String KILL_STRING = "---TWEAKSUITE-KILL-SWITCH---";
    public static final String SOCKET_IP = "127.0.0.1";
    public static final int SOCKET_PORT = 49277;
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
