package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class ClassLoaderGraphDefinitionImpl implements ClassLoaderGraphDefinition {
    private final Map<String, ClassLoaderNodeDefinition> nodes = new HashMap<>();
    @Getter
    private ClassLoader parentClassLoader = ClassLoaderGraphDefinitionImpl.class.getClassLoader();
    @Getter
    private ClassLoadingStrategy defaultLoadingStrategy = BaseStrategy.PIS;

    @Override
    public ClassLoaderNodeDefinition getNode(String name) {
        return nodes.get(name);
    }

    @Override
    public ClassLoaderNodeDefinition getOrCreateNode(@NonNull String name) {
        ClassLoaderNodeDefinition node = nodes.get(name);
        if (node == null) {
            node = new ClassLoaderNodeDefinitionImpl(name);
            nodes.put(name, node);
        }
        return node;
    }

    @Override
    public Collection<ClassLoaderNodeDefinition> getNodes() {
        return unmodifiableCollection(nodes.values());
    }

    @Override
    public ClassLoaderGraphDefinition setParentClassLoader(ClassLoader classLoader) {
        this.parentClassLoader = classLoader;
        return this;
    }

    @Override
    public ClassLoaderGraphDefinition setDefaultLoadingStrategy(@NonNull String strategy) {
        try {
            setDefaultLoadingStrategy(resolveStrategy(strategy));
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't resolve strategy name '" + strategy + "'", e);
        }
        return this;
    }

    @Override
    public ClassLoaderGraphDefinition setDefaultLoadingStrategy(@NonNull ClassLoadingStrategy strategy) {
        defaultLoadingStrategy = strategy;
        return this;
    }

    @Override
    public ClassLoaderGraph materialize() throws MaterializationException {
        return materialize(ClassLoaderGraphDefinitionImpl.class.getClassLoader());
    }

    @Override
    public ClassLoaderGraph materialize(@NonNull ClassLoader classLoader) throws MaterializationException {
        return new GraphMaterializer(this, classLoader).materialize();
    }

    private ClassLoadingStrategy resolveStrategy(String name) throws Exception {
        ClassLoadingStrategy strategy = BaseStrategy.byName(name.toUpperCase());
        if (strategy == null) {
            Class<?> clazz = ClassLoaderGraphDefinitionImpl.class.getClassLoader().loadClass(name);
            if (!ClassLoadingStrategy.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + name + " does not implement ClassLoadingStrategy interface");
            }
            strategy = ((ClassLoadingStrategy) clazz.getConstructor().newInstance());
        }
        return strategy;
    }

    @RequiredArgsConstructor
    private class ClassLoaderNodeDefinitionImpl implements ClassLoaderNodeDefinition {
        private final List<ImportDefinition> importClasses = new ArrayList<>();
        private final List<ImportDefinition> importResources = new ArrayList<>();
        private final Set<String> sources = new LinkedHashSet<>();
        @Getter
        private final String name;
        @Getter
        private ClassLoaderNodeDefinition parent;
        @Getter
        private ClassLoadingStrategy loadingStrategy;

        @Override
        public ClassLoaderGraphDefinition getGraph() {
            return ClassLoaderGraphDefinitionImpl.this;
        }

        @Override
        public ClassLoaderNodeDefinition addSource(@NonNull String path) {
            sources.add(path);
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition addSource(@NonNull List<String> paths) {
            for (String source : sources) {
                requireNonNull(source, "null path");
            }
            sources.addAll(paths);
            return this;
        }

        @Override
        public Collection<String> getSources() {
            return unmodifiableCollection(sources);
        }

        @Override
        public ClassLoaderNodeDefinition addChild(String name) {
            getOrCreateNode(name).setParent(this);
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition addChild(ClassLoaderNodeDefinition nodeDefinition) {
            checkSameGraph(nodeDefinition).parent = this;
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition setParent(String name) {
            this.parent = name == null ? null : getOrCreateNode(name);
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition setParent(ClassLoaderNodeDefinition parent) {
            this.parent = parent == null ? null : checkSameGraph(parent);
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition setLoadingStrategy(String strategy) {
            try {
                setLoadingStrategy(strategy == null ? null : resolveStrategy(strategy));
            } catch (Exception e) {
                throw new IllegalArgumentException("Can't resolve strategy name '" + strategy + "'", e);
            }
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition setLoadingStrategy(ClassLoadingStrategy strategy) {
            this.loadingStrategy = strategy;
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition addImportClasses(@NonNull ClassLoaderNodeDefinition from, @NonNull String path) {
            checkSameGraph(from);
            importClasses.add(new ImportDefinition(from, path));
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition addImportClasses(@NonNull String from, @NonNull String path) {
            importClasses.add(new ImportDefinition(getOrCreateNode(from), path));
            return this;
        }

        @Override
        public Collection<ImportDefinition> getImportClasses() {
            return unmodifiableList(importClasses);
        }

        @Override
        public ClassLoaderNodeDefinition addImportResources(@NonNull ClassLoaderNodeDefinition from, @NonNull String path) {
            checkSameGraph(from);
            importResources.add(new ImportDefinition(from, path));
            return this;
        }

        @Override
        public ClassLoaderNodeDefinition addImportResources(@NonNull String from, @NonNull String path) {
            importResources.add(new ImportDefinition(getOrCreateNode(from), path));
            return this;
        }

        @Override
        public Collection<ImportDefinition> getImportResources() {
            return unmodifiableList(importResources);
        }

        private ClassLoaderNodeDefinitionImpl checkSameGraph(ClassLoaderNodeDefinition node) {
            if (getNode(node.getName()) != node) {
                throw new IllegalArgumentException("Node " + node.getName() + " instance does not belong to this graph");
            }
            return (ClassLoaderNodeDefinitionImpl) node;
        }
    }
}
