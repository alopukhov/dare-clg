package io.github.alopukhov.dare.clg;

import java.util.Collection;

public interface ClassLoaderGraphDefinition {
    ClassLoaderNodeDefinition getNode(String name);

    ClassLoaderNodeDefinition getOrCreateNode(String name);

    Collection<ClassLoaderNodeDefinition> getNodes();

    ClassLoader getParentClassLoader();

    ClassLoaderGraphDefinition setParentClassLoader(ClassLoader classLoader);

    ClassLoadingStrategy getDefaultLoadingStrategy();

    ClassLoaderGraphDefinition setDefaultLoadingStrategy(String strategy);

    ClassLoaderGraphDefinition setDefaultLoadingStrategy(ClassLoadingStrategy strategy);

    ClassLoaderGraph materialize() throws MaterializationException;

    ClassLoaderGraph materialize(ClassLoader classLoader) throws MaterializationException;
}
