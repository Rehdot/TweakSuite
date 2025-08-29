package redot.tweaksuite.commons.model;

import java.util.LinkedList;
import java.util.List;

public class ThreadRegistry {

    public static final List<SuiteThread> REGISTRY = new LinkedList<>();

    public static void killProcesses() {
        killProcessesSafe();
        killProcessesUnsafe();
    }

    /// Disallows the threads from running. This approach only works
    /// if the user checks the SuiteThread's permit in their code.
    public static void killProcessesSafe() {
        REGISTRY.forEach(thread -> thread.setPermitted(false));
    }

    /// Tries to kill the threads at all costs
    public static void killProcessesUnsafe() {
        try {
            REGISTRY.forEach(Thread::stop);
            REGISTRY.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
