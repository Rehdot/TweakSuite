package redot.tweaksuite.client.model;

import com.google.common.collect.Sets;
import redot.tweaksuite.commons.model.SuiteClass;

import java.util.Set;

public class PermRegistry {

    private static final Set<String> REGISTRY = Sets.newHashSet();

    public static void addClass(SuiteClass suiteClass) {
        addClass(suiteClass.getClassName());
    }

    public static void addClass(String className) {
        REGISTRY.add(className);
    }

    public static boolean hasClass(SuiteClass suiteClass) {
        return hasClass(suiteClass.getClassName());
    }

    public static boolean hasClass(String className) {
        return REGISTRY.contains(className);
    }

}
