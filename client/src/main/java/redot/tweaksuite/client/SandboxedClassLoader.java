package redot.tweaksuite.client;

public class SandboxedClassLoader extends ClassLoader {

    public SandboxedClassLoader() {
        super(TweakSuiteClient.class.getClassLoader());
    }

}

