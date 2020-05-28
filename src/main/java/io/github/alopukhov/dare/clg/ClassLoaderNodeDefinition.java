package io.github.alopukhov.dare.clg;

import java.util.Collection;
import java.util.List;

public interface ClassLoaderNodeDefinition {
    String getName();

    ClassLoaderGraphDefinition getGraph();

    ClassLoaderNodeDefinition addSource(String path);

    ClassLoaderNodeDefinition addSource(List<String> paths);

    Collection<String> getSources();

    ClassLoaderNodeDefinition addChild(String name);

    ClassLoaderNodeDefinition addChild(ClassLoaderNodeDefinition nodeDefinition);

    ClassLoaderNodeDefinition getParent();

    ClassLoaderNodeDefinition setParent(ClassLoaderNodeDefinition parent);

    ClassLoadingStrategy getLoadingStrategy();

    ClassLoaderNodeDefinition setLoadingStrategy(String strategy);

    ClassLoaderNodeDefinition setLoadingStrategy(ClassLoadingStrategy strategy);

    ClassLoaderNodeDefinition addImportClasses(ClassLoaderNodeDefinition from, String path);

    ClassLoaderNodeDefinition addImportClasses(String from, String path);

    Collection<ImportDefinition> getImportClasses();

    ClassLoaderNodeDefinition addImportResources(ClassLoaderNodeDefinition from, String path);

    ClassLoaderNodeDefinition addImportResources(String from, String path);

    Collection<ImportDefinition> getImportResources();
}