package io.github.alopukhov.dare.clg;

import java.io.Closeable;
import java.util.Collection;

public interface ClassLoaderGraph extends Closeable {
    ClassLoader getParentClassLoader();

    ClassLoaderNode getNode(String name);

    Collection<ClassLoaderNode> getAllNodes();

    Collection<ClassLoaderNode> getOrphanNodes();
}
