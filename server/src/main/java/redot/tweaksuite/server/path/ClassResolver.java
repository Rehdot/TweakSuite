package redot.tweaksuite.server.path;

import java.util.Collection;

public interface ClassResolver {

    Collection<Class<?>> includedClasses();

    Collection<Object> includedObjects();

}
