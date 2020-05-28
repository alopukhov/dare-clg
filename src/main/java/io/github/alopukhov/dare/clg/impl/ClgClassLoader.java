package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoadingStrategy;
import io.github.alopukhov.dare.clg.ClassLoadingStrategy.LoadingUtil;
import lombok.NonNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptyList;

class ClgClassLoader extends URLClassLoader {
    private static final List<TargetImportItem> EMPTY_IMPORTS = emptyList();
    private final LoadingUtil loadingUtil = new LoadingUtilImpl();
    private final ClassLoadingStrategy loadingStrategy;
    private final List<TargetImportItem> classImports;
    private final List<TargetImportItem> resourceImports;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    ClgClassLoader(@NonNull URL[] urls, ClassLoader parent,
                   @NonNull ClassLoadingStrategy loadingStrategy,
                   List<TargetImportItem> classImports,
                   List<TargetImportItem> resourceImports) {
        super(urls, parent);
        this.loadingStrategy = loadingStrategy;
        this.classImports = classImports == null ? EMPTY_IMPORTS : classImports;
        this.resourceImports = resourceImports == null ? EMPTY_IMPORTS : resourceImports;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = loadingStrategy.loadClass(name, loadingUtil);
            }
            if (c == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    public URL getResource(String name) {
        return loadingStrategy.getResource(name, loadingUtil);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = loadingStrategy.getResources(name, loadingUtil);
        if (resources == null) {
            return emptyEnumeration();
        }
        return resources;
    }

    private class LoadingUtilImpl implements LoadingUtil {
        @Override
        public Class<?> findClassInParent(String name) {
            ClassLoader parent = getParent();
            try {
                return parent != null ? parent.loadClass(name) : Class.forName(name, false, null);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public Class<?> findClassInSelf(String name) {
            try {
                return ClgClassLoader.this.findClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public Class<?> findClassInImports(String name) {
            for (TargetImportItem clImport : classImports) {
                if (clImport.getImportItem().accepts(name)) {
                    try {
                        return clImport.getTarget().loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                    }
                }
            }
            return null;
        }

        @Override
        public URL findResourceInParent(String name) {
            ClassLoader parent = getParent();
            if (parent != null) {
                return parent.getResource(name);
            }
            return null;
        }

        @Override
        public URL findResourceInSelf(String name) {
            return ClgClassLoader.this.findResource(name);
        }

        @Override
        public URL findResourceInImports(String name) {
            for (TargetImportItem resourceImport : resourceImports) {
                if (resourceImport.getImportItem().accepts(name)) {
                    URL resource = resourceImport.getTarget().getResource(name);
                    if (resource != null) {
                        return resource;
                    }
                }
            }
            return null;
        }

        @Override
        public Enumeration<URL> findResourcesInParent(String name) throws IOException {
            ClassLoader parent = getParent();
            if (parent != null) {
                return parent.getResources(name);
            }
            return emptyEnumeration();
        }

        @Override
        public Enumeration<URL> findResourcesInSelf(String name) throws IOException {
            return ClgClassLoader.this.findResources(name);
        }

        @Override
        public Enumeration<URL> findResourcesInImports(String name) throws IOException {
            List<Enumeration<URL>> resources = new ArrayList<>();
            for (TargetImportItem resourceImport : resourceImports) {
                if (resourceImport.getImportItem().accepts(name)) {
                    resources.add(resourceImport.getTarget().getResources(name));
                }
            }
            if (resources.isEmpty()) {
                return emptyEnumeration();
            }
            return new CompoundEnumeration<>(resources.iterator());
        }
    }
}
