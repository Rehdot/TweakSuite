package redot.tweaksuite.server.compile;

import net.openhft.compiler.CachedCompiler;
import redot.tweaksuite.commons.annotation.Entrypoint;
import redot.tweaksuite.commons.annotation.Inject;
import redot.tweaksuite.commons.annotation.This;
import redot.tweaksuite.commons.inject.resolve.MethodFinder;
import redot.tweaksuite.commons.inject.method.MethodInjector;
import redot.tweaksuite.commons.util.StringUtil;
import redot.tweaksuite.server.TweakSuite;
import redot.tweaksuite.server.data.SuiteClass;
import redot.tweaksuite.commons.model.SuiteThread;
import redot.tweaksuite.server.log.SuiteWriter;
import redot.tweaksuite.server.registry.PermRegistry;

import javax.tools.JavaFileObject;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class SuiteCompiler {

    private final Constructor<?> jsfsConstructor;
    private final Field jfoField;
    private final File permClassDir;
    private final CachedCompiler permCompiler;
    private final TweakSuite tweakSuite;

    public SuiteCompiler(TweakSuite tweakSuite) {
        this.tweakSuite = tweakSuite;

        try {
            this.permClassDir = File.createTempFile("tweaksuite-perm", "");
            if (this.permClassDir.exists()) this.permClassDir.delete();
            this.permClassDir.mkdirs();
            this.permClassDir.deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp perm class directory", e);
        }

        try {
            this.jfoField = CachedCompiler.class.getDeclaredField("javaFileObjects");
            this.jfoField.setAccessible(true);

            Class<?> clazz = Class.forName("net.openhft.compiler.JavaSourceFromString");
            this.jsfsConstructor = clazz.getDeclaredConstructor(String.class, String.class);
            this.jsfsConstructor.setAccessible(true);

            this.permCompiler = new CachedCompiler(null, this.permClassDir, List.of("-g", "-nowarn",
                    "-classpath", System.getProperty("java.class.path") + File.pathSeparator + this.permClassDir.getAbsolutePath()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void compileClasses(List<String> tempClassDefs, List<String> permClassDefs) {
        new Thread(() -> {// todo remove debug
            System.out.println("DEBUG: " + tempClassDefs.toString() + "\n" + permClassDefs.toString());

            List<SuiteClass> tempSuiteClasses = this.createSuiteClasses(tempClassDefs);
            List<SuiteClass> permSuiteClasses = this.createSuiteClasses(permClassDefs);
            SuiteWriter writer = new SuiteWriter(this.tweakSuite, tempClassDefs, permClassDefs);

            this.compileAndLoadPermClasses(permSuiteClasses, writer);
            this.compileAndLoadTempClasses(tempSuiteClasses, writer);

            this.injectMethods(permSuiteClasses);
            this.injectMethods(tempSuiteClasses);

            this.runEntrypoints(permSuiteClasses);
            this.runEntrypoints(tempSuiteClasses);
        }, "TweakSuiteCompiler").start();
    }

    private List<SuiteClass> createSuiteClasses(List<String> classDefs) {
        List<SuiteClass> suiteClasses = new ArrayList<>();

        for (String classDef : classDefs) {
            SuiteClass suiteClass = new SuiteClass();
            suiteClass.setClassDef(classDef);
            suiteClass.setClassName(StringUtil.extractClassName(classDef));
            suiteClasses.add(suiteClass);
        }

        return suiteClasses;
    }

    private void compileAndLoadPermClasses(List<SuiteClass> classes, SuiteWriter writer) {
        ClassLoader loader = this.tweakSuite.getBaseClassLoader();
        PermRegistry permRegistry = this.tweakSuite.getPermRegistry();

        classes.removeIf(suiteClass -> {
            return permRegistry.hasClass(suiteClass.getClassName());
        });

        populateAndLoadClasses(classes, this.permCompiler, loader, writer);

        for (SuiteClass suiteClass : classes) {
            permRegistry.addClass(suiteClass.getClassName());
        }
    }

    private void compileAndLoadTempClasses(List<SuiteClass> classes, SuiteWriter writer) {
        CachedCompiler compiler = createTempCompiler();
        ClassLoader loader = new SandboxedClassLoader(this.tweakSuite);

        populateAndLoadClasses(classes, compiler, loader, writer);
    }

    private void populateAndLoadClasses(List<SuiteClass> classes, CachedCompiler compiler, ClassLoader loader, SuiteWriter writer) {
        populateCompilerJavaFileObjects(classes, compiler);

        if (!classes.isEmpty()) {
            loadLeadClass(classes.get(0), compiler, loader, writer);
        }

        for (SuiteClass suiteClass : classes) {
            Class<?> loadedClass = loadClass(loader, suiteClass.getClassName());
            suiteClass.setLiteralClass(loadedClass);
            if (loadedClass == null) {
                this.tweakSuite.getLogger().warn("Class '{}' was null during compilation.", suiteClass.getClassName());
            }
        }
    }

    private void loadLeadClass(SuiteClass leadClass, CachedCompiler compiler, ClassLoader loader, SuiteWriter writer) {
        try {
            compiler.loadFromJava(loader, leadClass.getClassName(), leadClass.getClassDef(), writer);
        } catch (Exception e) {
            this.tweakSuite.getLogger().error("Failed to load lead class '{}'\n{}", leadClass.getClassName(), e.toString());
        }
    }

    private Class<?> loadClass(ClassLoader loader, String className) {
        try {
            return loader.loadClass(className);
        } catch (Exception e) {
            this.tweakSuite.getLogger().error("Failed to load class '{}'\n{}", className, e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void populateCompilerJavaFileObjects(List<SuiteClass> classes, CachedCompiler compiler) {
        try {
            var javaFileObjects = (ConcurrentMap<String, JavaFileObject>) jfoField.get(compiler);
            for (SuiteClass suiteClass : classes) {
                if (this.tweakSuite.getPermRegistry().hasClass(suiteClass.getClassName())) continue;
                JavaFileObject jfo = (JavaFileObject) jsfsConstructor.newInstance(suiteClass.getClassName(), suiteClass.getClassDef());
                javaFileObjects.put(suiteClass.getClassName(), jfo);
            }
        } catch (Exception e) {
            this.tweakSuite.getLogger().error("Failed to populate JavaFileObjects: {}", e.toString());
        }
    }

    private void runEntrypoints(List<SuiteClass> classes) {
        for (SuiteClass suiteClass : classes) {
            Class<?> clazz = suiteClass.getLiteralClass();
            if (clazz == null) continue;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Entrypoint.class)) {
                    SuiteThread thread = createInvokerThread(method);
                    this.tweakSuite.getThreadRegistry().addProcess(thread);
                    thread.start();
                }
            }
        }
    }

    private void injectMethods(List<SuiteClass> classes) {
        for (SuiteClass suiteClass : classes) {
            Class<?> clazz = suiteClass.getLiteralClass();
            if (clazz == null) continue;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Inject.class)) {
                    this.injectMethod(method, method.getAnnotation(Inject.class));
                }
            }
        }
    }

    private void injectMethod(Method method, Inject annotation) {
        Class<?>[] paramsWithoutThis = Arrays.stream(method.getParameters()).filter(p -> {
            return !p.isAnnotationPresent(This.class);
        }).map(Parameter::getType).toArray(Class<?>[]::new);

        MethodFinder finder = new MethodFinder(annotation.value())
                .withParamTypes(paramsWithoutThis)
                .withReturnType(method.getReturnType())
                .withName(annotation.name())
                .withCount(annotation.count());

        new MethodInjector(finder)
                .withFunctionality(method)
                .withInjectionPoint(annotation.point())
                .inject();
    }

    private CachedCompiler createTempCompiler() {
        return new CachedCompiler(null, null, List.of("-g", "-nowarn", "-classpath",
                System.getProperty("java.class.path") + File.pathSeparator + this.permClassDir.getAbsolutePath())
        );
    }

    private SuiteThread createInvokerThread(Method method) {
        AtomicReference<SuiteThread> threadReference = new AtomicReference<>();
        SuiteThread thread = new SuiteThread(() -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                this.tweakSuite.getLogger().error("Failed executing Entrypoint: {}\n{}", e.getMessage(), e.toString());
            }
            this.tweakSuite.getThreadRegistry().remove(threadReference.get());
        }, "TweakSuiteInvoker");
        threadReference.set(thread);
        return thread;
    }

}
