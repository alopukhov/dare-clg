package io.github.alopukhov.dare.clg;

import io.github.alopukhov.dare.clg.impl.ClassLoaderGraphDefinitionImpl;

public class ClassloaderGraphFactory {
    public static ClassLoaderGraphDefinition defineNewGraph() {
        return new ClassLoaderGraphDefinitionImpl();
    }
}
