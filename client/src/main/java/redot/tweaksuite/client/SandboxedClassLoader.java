package redot.tweaksuite.client;

/// A ClassLoader which (importantly) can be garbage-collected.
/// This allows TweakSuite classes to re-use their old names upon every compilation.
public class SandboxedClassLoader extends ClassLoader {

    public SandboxedClassLoader() {
        super(TweakSuiteClient.getBaseClassLoader());
    }

}

