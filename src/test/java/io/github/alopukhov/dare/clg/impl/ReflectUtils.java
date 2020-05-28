package io.github.alopukhov.dare.clg.impl;

public class ReflectUtils {
    public static Object newInstance(ClassLoader cl, String className) throws Exception {
        return cl.loadClass(className).getConstructor().newInstance();
    }
}
