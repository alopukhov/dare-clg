package io.github.alopukhov.dare.clg;

import java.util.Collection;

public interface ClassLoaderNode {
    String getName();

    ClassLoader getClassLoader();

    ClassLoaderNode getParent();

    ClassLoaderNode getChild(String name);

    Collection<ClassLoaderNode> getChildren();
}
