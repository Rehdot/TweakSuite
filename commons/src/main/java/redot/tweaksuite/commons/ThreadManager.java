package redot.tweaksuite.commons;

import java.util.concurrent.TimeUnit;

/// A utility class to be used in user-written runtime code.
public class ThreadManager {

    /// Begs the thread registry for the thread's life
    public static void beg() {
        if (!permits()) {
            throw new RuntimeException("Thread was forcefully ended by TweakSuite.");
        }
    }

    /// @return Whether the current thread is permitted by the thread registry
    public static boolean permits() {
        try {
            return ((SuiteThread) Thread.currentThread()).isPermitted();
        } catch (ClassCastException ignored) {}
        return true;
    }

    public static void sleepSec(long seconds) {
        sleep(seconds, TimeUnit.SECONDS);
    }

    public static void sleepMS(long milliseconds) {
        sleep(milliseconds, TimeUnit.MILLISECONDS);
    }

    /// Sleeps current thread for amount of provided TimeUnit
    public static void sleep(long timeout, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(timeout);
        } catch (Exception ignored) {}
    }

}
