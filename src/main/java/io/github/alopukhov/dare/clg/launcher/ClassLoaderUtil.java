package io.github.alopukhov.dare.clg.launcher;

public class ClassLoaderUtil {
    public static ClassLoader byName(String name) {
        switch (name.toLowerCase()) {
            case "system":
                return ClassLoader.getSystemClassLoader();
            case "context":
                return Thread.currentThread().getContextClassLoader();
        }
        throw new IllegalArgumentException("Unknown classloader alias [" + name + "]");
    }
}
