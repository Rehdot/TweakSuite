package redot.tweaksuite.server.log;

public class DefaultLogger implements SuiteLogger {

    @Override
    public void info(String s) {
        System.out.println(s);
    }

    @Override
    public void warn(String s) {
        System.out.println("Warning: " + s);
    }

    @Override
    public void error(String s) {
        System.err.println(s);
    }

}
