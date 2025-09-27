package redot.tweaksuite.commons.annotation;

import redot.tweaksuite.commons.inject.method.InjectionPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    Class<?> value();

    int count() default 1;

    String name() default "";

    InjectionPoint point() default InjectionPoint.ENTRANCE;

}
