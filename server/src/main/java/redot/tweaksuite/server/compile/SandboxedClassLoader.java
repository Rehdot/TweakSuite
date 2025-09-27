package redot.tweaksuite.server.compile;


import redot.tweaksuite.server.TweakSuite;

/// A ClassLoader which (importantly) can be garbage-collected.
/// This allows TweakSuite classes to re-use their old names upon every compilation.
public class SandboxedClassLoader extends ClassLoader {

    private final TweakSuite tweakSuite;

    public SandboxedClassLoader(TweakSuite tweakSuite) {
        super(tweakSuite.getBaseClassLoader());
        this.tweakSuite = tweakSuite;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (this.tweakSuite.getPermRegistry().hasClass(name)) {
            return this.tweakSuite.getBaseClassLoader().loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

}

