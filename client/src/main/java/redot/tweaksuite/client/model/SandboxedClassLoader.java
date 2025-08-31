package redot.tweaksuite.client.model;

import redot.tweaksuite.client.TweakSuiteClient;

/// A ClassLoader which (importantly) can be garbage-collected.
/// This allows TweakSuite classes to re-use their old names upon every compilation.
public class SandboxedClassLoader extends ClassLoader {

    public SandboxedClassLoader() {
        super(TweakSuiteClient.getBaseClassLoader());
    }

    /// Enables support for class permanence
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (PermRegistry.hasClass(name)) {
            return TweakSuiteClient.getBaseClassLoader().loadClass(name);
        }
        return super.loadClass(name, resolve);
    }


}

