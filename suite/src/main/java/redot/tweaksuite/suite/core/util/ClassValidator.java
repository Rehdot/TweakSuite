package redot.tweaksuite.suite.core.util;

import com.google.common.collect.Sets;
import org.objectweb.asm.*;
import redot.tweaksuite.commons.Entrypoint;
import redot.tweaksuite.commons.Permanent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassValidator {

    public static final Set<String> PERM_CLASS_NAMES = Sets.newHashSet();
    private static final String PERM_DESCRIPTOR = Type.getDescriptor(Permanent.class);
    private static final String ENTRYPOINT_DESCRIPTOR = Type.getDescriptor(Entrypoint.class);

    public static void validateJar(String jarPath) {
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

    private static class SuiteClassVisitor extends ClassVisitor {
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
                    boolean isEntrypoint = annotationDescriptor.equals(ENTRYPOINT_DESCRIPTOR);
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
            if (descriptor.equals(PERM_DESCRIPTOR)) {
                String simpleClassName = this.getSimpleClassName();
                PERM_CLASS_NAMES.add(simpleClassName);
            }
            return super.visitAnnotation(descriptor, visible);
        }

        private String getSimpleClassName() {
            String[] split = this.className.split("/");
            return split[split.length - 1];
        }
    }
}
