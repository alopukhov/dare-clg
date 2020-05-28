package io.github.alopukhov.dare.clg;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public interface ClassLoadingStrategy {
    Class<?> loadClass(String className, LoadingUtil loadingUtil) throws ClassNotFoundException;

    URL getResource(String name, LoadingUtil loadingUtil);

    Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException;

    interface LoadingUtil {
        Class<?> findClassInParent(String name);

        Class<?> findClassInSelf(String name);

        Class<?> findClassInImports(String name);

        URL findResourceInParent(String name);

        URL findResourceInSelf(String name);

        URL findResourceInImports(String name);

        Enumeration<URL> findResourcesInParent(String name) throws IOException;

        Enumeration<URL> findResourcesInSelf(String name) throws IOException;

        Enumeration<URL> findResourcesInImports(String name) throws IOException;
    }
}