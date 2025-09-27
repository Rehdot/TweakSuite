package redot.tweaksuite.commons.inject.resolve;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodFinder implements Finder<Method> {

    @Getter
    private final Class<?> declaringClass;
    private Class<?>[] paramTypes;
    private Class<?> returnType;
    private String name;
    private int count;

    public MethodFinder(Class<?> declaringClass) {
        this.declaringClass = declaringClass;
        this.paramTypes = new Class<?>[]{};
        this.returnType = void.class;
        this.name = "";
        this.count = 1;
    }

    public MethodFinder withReturnType(Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    public MethodFinder withParamTypes(Class<?>... paramTypes) {
        this.paramTypes = paramTypes;
        return this;
    }

    /// The name of the method (optional)
    public MethodFinder withName(String name) {
        this.name = name;
        return this;
    }

    /// The nth method which matches the search
    public MethodFinder withCount(int count) {
        this.count = count;
        return this;
    }

    @Override
    public Method find() {
        int i = 0;

        for (Method method : this.declaringClass.getDeclaredMethods()) {
            if (method.getReturnType().equals(this.returnType)
                    && Arrays.equals(this.paramTypes, method.getParameterTypes())
                    && (this.name.isEmpty() || this.name.equals(method.getName()))) {
                if (++i >= this.count) {
                    return method;
                }
            }
        }

        throw new RuntimeException("Failed to find a method with search:\nName: " + this.name + "\nDeclaring class: " +
                this.declaringClass + "\nReturn type: " + this.returnType + "\nParameter types: " + Arrays.toString(this.paramTypes));
    }

}
