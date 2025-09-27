package redot.tweaksuite.server.registry;

import redot.tweaksuite.server.data.SuiteClass;

import java.util.HashSet;
import java.util.Set;

public class PermRegistry {

    private final Set<String> registry = new HashSet<>();

    public void addClass(SuiteClass suiteClass) {
        this.addClass(suiteClass.getClassName());
    }

    public void addClass(String className) {
        this.registry.add(className);
    }

    public boolean hasClass(SuiteClass suiteClass) {
        return this.hasClass(suiteClass.getClassName());
    }

    public boolean hasClass(String className) {
        return this.registry.contains(className);
    }

}
