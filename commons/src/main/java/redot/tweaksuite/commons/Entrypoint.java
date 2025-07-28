package redot.tweaksuite.commons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// An annotation to be used to start the execution process
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entrypoint {

}
