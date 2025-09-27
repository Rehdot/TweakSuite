package redot.tweaksuite.commons.inject.method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import redot.tweaksuite.commons.annotation.This;
import redot.tweaksuite.commons.inject.Injector;
import redot.tweaksuite.commons.inject.resolve.MethodFinder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodInjector implements Injector {

    /// I wish there was a non-static solution to this
    private static final Map<Method, MethodInjector> registry = new ConcurrentHashMap<>();

    private final MethodFinder finder;
    private InjectionPoint point;
    private Method functionality;


    public MethodInjector(MethodFinder finder) {
        this.finder = finder;
        this.point = InjectionPoint.ENTRANCE;
        this.functionality = null;
    }

    @Override
    public void inject() {
        Method target = this.finder.find();
        Class<?> declaring = this.finder.getDeclaringClass();
        Class<?> advice = switch (this.point) {
            case RETURN -> MethodAdvice.Exit.class;
            case ENTRANCE -> MethodAdvice.Entrance.class;
        };

        register(target, this);

        new ByteBuddy()
                .redefine(declaring)
                .visit(Advice.to(advice).on(ElementMatchers.is(target)))
                .make()
                .load(declaring.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    public MethodInjector withInjectionPoint(InjectionPoint point) {
        this.point = point;
        return this;
    }

    public MethodInjector withFunctionality(Method functionality) {
        this.functionality = functionality;
        return this;
    }

    public Object executeFunctionality(Object self, Object... args) {
        List<Object> finalArgs = new ArrayList<>();
        int argCount = 0;

        for (Parameter parameter : this.functionality.getParameters()) {
            if (parameter.isAnnotationPresent(This.class)) {
                finalArgs.add(self);
            } else {
                finalArgs.add(args[argCount++]);
            }
        }

        try {
            // functionality is a static method that lives somewhere else
            return this.functionality.invoke(null, finalArgs.toArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void register(Method target, MethodInjector injector) {
        registry.put(target, injector);
    }

    public static void unregister(Method target) {
        registry.remove(target);
    }

    public static MethodInjector resolve(Method method) {
        return registry.get(method);
    }

}
