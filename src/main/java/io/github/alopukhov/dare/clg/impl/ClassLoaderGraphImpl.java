package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.ClassLoaderGraph;
import io.github.alopukhov.dare.clg.ClassLoaderNode;
import io.github.alopukhov.dare.clg.spi.ResourceHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

@Slf4j
class ClassLoaderGraphImpl implements ClassLoaderGraph {
    @Getter
    private final ClassLoader parentClassLoader;
    private final Map<String, ClassLoaderNode> nodes;
    private final List<ClassLoaderNode> orphans;
    private final Collection<ResourceHandler> handlers;

    public ClassLoaderGraphImpl(ClassLoader parentClassLoader, Collection<? extends ClassLoaderNode> nodes, Collection<ResourceHandler> handlers) {
        this.parentClassLoader = parentClassLoader;
        this.nodes = toMap(nodes);
        this.orphans = findOrphans(nodes);
        this.handlers = handlers;
    }

    @Override
    public ClassLoaderNode getNode(String name) {
        return nodes.get(name);
    }

    @Override
    public Collection<ClassLoaderNode> getOrphanNodes() {
        return unmodifiableList(orphans);
    }

    @Override
    public Collection<ClassLoaderNode> getAllNodes() {
        return unmodifiableCollection(nodes.values());
    }

    @Override
    public synchronized void close() throws IOException {
        Exception toThrow = null;
        for (ClassLoaderNode node : nodes.values()) {
            ClassLoader nodeCl = node.getClassLoader();
            if (nodeCl instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) nodeCl).close();
                } catch (Exception e) {
                    log.error("Error closing classloader of node [{}]", node.getName(), e);
                    toThrow = addSuppressed(toThrow, e);
                }
            }
        }
        for (ResourceHandler handler : handlers) {
            try {
                handler.close();
            } catch (Exception e) {
                log.error("Error closing resource handler [{}]", handler, e);
                toThrow = addSuppressed(toThrow, e);
            }
        }
        if (toThrow != null) {
            throw new IOException("Can't release all resource", toThrow);
        }
    }

    private static Exception addSuppressed(Exception target, Exception suppressed) {
        if (target == null) {
            return suppressed;
        }
        target.addSuppressed(suppressed);
        return target;
    }

    private static Map<String, ClassLoaderNode> toMap(Collection<? extends ClassLoaderNode> nodes) {
        HashMap<String, ClassLoaderNode> map = new HashMap<>(nodes.size());
        for (ClassLoaderNode node : nodes) {
            ClassLoaderNode oldOne = map.put(node.getName(), node);
            if (oldOne != null) {
                throw new IllegalArgumentException("duplicate node name " + node.getName());
            }
        }
        return map;
    }

    private static List<ClassLoaderNode> findOrphans(Collection<? extends ClassLoaderNode> nodes) {
        List<ClassLoaderNode> orphans = new ArrayList<>();
        for (ClassLoaderNode node : nodes) {
            if (node.getParent() == null) {
                orphans.add(node);
            }
        }
        return orphans;
    }
}
