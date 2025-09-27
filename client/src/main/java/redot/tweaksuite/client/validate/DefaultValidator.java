package redot.tweaksuite.client.validate;

import lombok.Getter;
import net.bytebuddy.jar.asm.*;
import redot.tweaksuite.commons.annotation.Entrypoint;
import redot.tweaksuite.commons.annotation.Permanent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DefaultValidator implements ClassValidator {

    @Getter
    private final Set<String> permClassNames = new HashSet<>();
    private final String permDescriptor = Type.getDescriptor(Permanent.class);
    private final String entrypointDescriptor = Type.getDescriptor(Entrypoint.class);

    @Override
    public void validateJar(String jarPath) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            for (JarEntry entry : jarFile.stream().toList()) {
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream is = jarFile.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(is);
                    reader.accept(new SuiteClassVisitor(reader.getClassName()), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SuiteClassVisitor extends ClassVisitor {
        private final String className;

        public SuiteClassVisitor(String className) {
            super(Opcodes.ASM9);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature, String[] exceptions) {
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            int paramCount = Type.getMethodType(methodDescriptor).getArgumentTypes().length;

            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    boolean isEntrypoint = annotationDescriptor.equals(entrypointDescriptor);
                    if (isEntrypoint && !isStatic) {
                        System.err.println("Entrypoint method '" + name + "' is not static. Entrypoint methods must be static. Descriptor: " + methodDescriptor);
                    }
                    if (isEntrypoint && (paramCount > 0)) {
                        System.err.println("Entrypoint method '" + name + "' has parameters. Entrypoint methods must not have parameters. Descriptor: " + methodDescriptor);
                    }
                    return super.visitAnnotation(annotationDescriptor, visible);
                }
            };
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(permDescriptor)) {
                String simpleClassName = this.getSimpleClassName();
                permClassNames.add(simpleClassName);
            }
            return super.visitAnnotation(descriptor, visible);
        }

        private String getSimpleClassName() {
            String[] split = this.className.split("/");
            return split[split.length - 1];
        }
    }
}