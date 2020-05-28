package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoaderNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;

@RequiredArgsConstructor
class ClassLoaderNodeImpl implements ClassLoaderNode {
    @Getter
    @NonNull
    private final String name;
    @Getter
    private final ClassLoaderNode parent;
    private final Map<String, ClassLoaderNode> children = new HashMap<>();
    @Getter
    @NonNull
    private final ClassLoader classLoader;

    @Override
    public ClassLoaderNode getChild(String name) {
        return children.get(name);
    }

    @Override
    public Collection<ClassLoaderNode> getChildren() {
        return unmodifiableCollection(children.values());
    }

    void registerChild(ClassLoaderNode node) {
        children.put(node.getName(), node);
    }
}
