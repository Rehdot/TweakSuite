package redot.tweaksuite.commons.util;

import lombok.experimental.UtilityClass;
import redot.tweaksuite.commons.model.SuiteThread;

import java.util.concurrent.TimeUnit;

/// A utility class to be used in user-written runtime code.
@UtilityClass
public class ThreadManager {

    /// Begs the thread registry for the thread's life
    public static void beg() {
        if (!permits()) {
            throw new RuntimeException("Thread was forcefully ended by TweakSuite.");
        }
    }

    /// @return Whether the current thread is permitted by the thread registry
    public static boolean permits() {
        if (Thread.currentThread() instanceof SuiteThread thread) {
            return thread.isPermitted();
        }
        return true;
    }

    public static boolean sleepSec(long seconds) {
        sleep(seconds, TimeUnit.SECONDS);
        return permits();
    }

    public static boolean sleepMS(long milliseconds) {
        sleep(milliseconds, TimeUnit.MILLISECONDS);
        return permits();
    }

    public static boolean sleep(long timeout, TimeUnit timeUnit) {
        sleep(timeout, timeUnit, true);
        return permits();
    }

    /// Sleeps current thread for amount of provided TimeUnit
    /// @param beg Whether the sleep call should beg afterward
    public static void sleep(long timeout, TimeUnit timeUnit, boolean beg) {
        try {
            timeUnit.sleep(timeout);
        } catch (Exception ignored) {}
        if (beg) beg();
    }

}
