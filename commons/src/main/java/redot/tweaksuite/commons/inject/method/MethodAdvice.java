package redot.tweaksuite.commons.inject.method;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class MethodAdvice {

    public static class Entrance {

        @Advice.OnMethodEnter
        public static void enter(@Advice.Origin Method method,
                                 @Advice.This(optional = true) Object self,
                                 @Advice.AllArguments Object[] args) {
            MethodInjector injector = MethodInjector.resolve(method);
            if (injector != null) {
                injector.executeFunctionality(self, args);
            }
        }

    }

    public static class Exit {

        @Advice.OnMethodExit
        public static void exit(@Advice.Origin Method method,
                                @Advice.This(optional = true) Object self,
                                @Advice.AllArguments Object[] args) {
            MethodInjector injector = MethodInjector.resolve(method);
            if (injector != null) {
                injector.executeFunctionality(self, args);
            }
        }

    }

}
