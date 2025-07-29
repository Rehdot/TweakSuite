package redot.tweaksuite.commons;

public class ThreadManager {

    /// Begs the thread registry for the thread's life
    public static void beg() throws RuntimeException {
        try {
            if (((SuiteThread) Thread.currentThread()).isPermitted()) return;
            throw new RuntimeException("Thread was forcefully ended by TweakSuite.");
        } catch (ClassCastException ignored) {}
    }

}
