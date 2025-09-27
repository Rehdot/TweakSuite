package redot.tweaksuite.server.registry;

import redot.tweaksuite.commons.model.SuiteThread;

import java.util.HashSet;
import java.util.Set;

public class ThreadRegistry {

    private final Set<SuiteThread> registry = new HashSet<>();

    public void addProcess(SuiteThread thread) {
        this.registry.add(thread);
    }

    public void remove(SuiteThread thread) {
        this.registry.remove(thread);
    }

    public void clear() {
        this.registry.clear();
    }

    public void killProcesses() {
        this.killProcessesSafe();
        this.killProcessesUnsafe();
        this.clear();
    }

    /// Disallows the threads from running. This approach only works
    /// if the user checks the SuiteThread's permit in their code.
    public void killProcessesSafe() {
        this.registry.forEach(thread -> thread.setPermitted(false));
    }

    /// Tries to kill the threads at all costs
    @SuppressWarnings("all")
    public void killProcessesUnsafe() {
        try {
            this.registry.forEach(Thread::stop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
