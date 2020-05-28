package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoadingStrategy;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;


@RequiredArgsConstructor
enum BaseStrategies implements ClassLoadingStrategy {
    PIS(SingleHelper.P, SingleHelper.I, SingleHelper.S),
    PSI(SingleHelper.P, SingleHelper.S, SingleHelper.I),
    SIP(SingleHelper.S, SingleHelper.I, SingleHelper.P),
    SPI(SingleHelper.S, SingleHelper.P, SingleHelper.I),
    IPS(SingleHelper.I, SingleHelper.P, SingleHelper.S),
    ISP(SingleHelper.I, SingleHelper.S, SingleHelper.P);

    private final SingleHelper first;
    private final SingleHelper second;
    private final SingleHelper third;

    @Override
    public Class<?> loadClass(String className, LoadingUtil loadingUtil) {
        Class<?> clazz = first.loadClass(className, loadingUtil);
        if (clazz == null) {
            clazz = second.loadClass(className, loadingUtil);
            if (clazz == null) {
                clazz = third.loadClass(className, loadingUtil);
            }
        }
        return clazz;
    }

    @Override
    public URL getResource(String name, LoadingUtil loadingUtil) {
        URL resource = first.getResource(name, loadingUtil);
        if (resource == null) {
            resource = second.getResource(name, loadingUtil);
            if (resource == null) {
                resource = third.getResource(name, loadingUtil);
            }
        }
        return resource;
    }

    @Override
    public Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException {
        Enumeration<URL> r1 = first.getResources(name, loadingUtil);
        Enumeration<URL> r2 = second.getResources(name, loadingUtil);
        Enumeration<URL> r3 = third.getResources(name, loadingUtil);
        return new CompoundEnumeration<>(Arrays.asList(r1, r2, r3).iterator());
    }

    private enum SingleHelper {
        P {
            @Override
            public Class<?> loadClass(String className, LoadingUtil loadingUtil) {
                return loadingUtil.findClassInParent(className);
            }

            @Override
            public URL getResource(String name, LoadingUtil loadingUtil) {
                return loadingUtil.findResourceInParent(name);
            }

            @Override
            public Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException {
                return loadingUtil.findResourcesInParent(name);
            }
        },
        S {
            @Override
            public Class<?> loadClass(String className, LoadingUtil loadingUtil) {
                return loadingUtil.findClassInSelf(className);
            }

            @Override
            public URL getResource(String name, LoadingUtil loadingUtil) {
                return loadingUtil.findResourceInSelf(name);
            }

            @Override
            public Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException {
                return loadingUtil.findResourcesInSelf(name);
            }
        },
        I {
            @Override
            public Class<?> loadClass(String className, LoadingUtil loadingUtil) {
                return loadingUtil.findClassInImports(className);
            }

            @Override
            public URL getResource(String name, LoadingUtil loadingUtil) {
                return loadingUtil.findResourceInImports(name);
            }

            @Override
            public Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException {
                return loadingUtil.findResourcesInImports(name);
            }
        };

        public abstract Class<?> loadClass(String className, LoadingUtil loadingUtil);

        public abstract URL getResource(String name, LoadingUtil loadingUtil);

        public abstract Enumeration<URL> getResources(String name, LoadingUtil loadingUtil) throws IOException;
    }
}
