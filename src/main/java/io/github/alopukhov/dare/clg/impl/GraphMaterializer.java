package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.*;
import io.github.alopukhov.dare.clg.impl.validation.NoCycles;
import io.github.alopukhov.dare.clg.spi.ResourceHandler;
import io.github.alopukhov.dare.clg.spi.SourceResolver;
import io.github.alopukhov.dare.clg.spi.UrlHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import static io.github.alopukhov.dare.clg.impl.ImportItem.createClassImport;
import static io.github.alopukhov.dare.clg.impl.ImportItem.createResourceImport;
import static java.util.Collections.reverse;

@RequiredArgsConstructor
@Slf4j
class GraphMaterializer {
    private final ClassLoaderGraphDefinition graphDefinition;
    private final ClassLoader classLoader;
    private Map<String, ClassLoaderNodeImpl> nodes;
    private Map<String, List<TargetImportItem>> allImports;
    private List<SourceResolver> resolvers;
    private final List<ResourceHandler> resourceHandlers = new ArrayList<>();
    private boolean materialized = false;


    public ClassLoaderGraph materialize() throws MaterializationException {
        if (materialized) {
            throw new IllegalStateException("Already called materialize");
        }
        materialized = true;
        try {
            nodes = new HashMap<>(graphDefinition.getNodes().size());
            allImports = new HashMap<>(graphDefinition.getNodes().size());
            log.debug("Materializing class loader graph...");
            validateGraph();
            resolvers = getResolvers();
            Collection<ClassLoaderNodeImpl> nodes = createNodes();
            List<ResourceHandler> reversedHandlers = new ArrayList<>(resourceHandlers);
            reverse(reversedHandlers);
            return new ClassLoaderGraphImpl(graphDefinition.getParentClassLoader(), nodes, reversedHandlers);
        } catch (Exception e) {
            log.error("Can't materialize graph. Closing resource handlers");
            for (ResourceHandler handler : resourceHandlers) {
                try {
                    handler.close();
                } catch (Exception closeException) {
                    log.error("Can't close resource handler {}", handler, e);
                    e.addSuppressed(closeException);
                }
            }
            throw e;
        }
    }

    private void validateGraph() throws MaterializationException {
        log.debug("Validating graph definition...");
        ValidationResult validationResult = new NoCycles().validate(graphDefinition);
        if (!validationResult.isSuccessful()) {
            log.error("Validation failed.\n{}", validationResult);
            throw new MaterializationException("Can't materialize graph with cycles");
        } else {
            log.debug("Graph definition validated:\n{}", validationResult);
        }
    }

    private List<SourceResolver> getResolvers() {
        List<SourceResolver> resolvers = new ArrayList<>();
        for (SourceResolver resolver : ServiceLoader.load(SourceResolver.class, classLoader)) {
            log.debug("Found resolver {} of class {}", resolver, resolver.getClass());
            registerPossibleHandler(resolver);
            resolvers.add(resolver);
        }
        resolvers.add(new DefaultSourceResolver());
        log.debug("Got {} new resolvers in total including default one.", resolvers.size());
        return resolvers;
    }

    private Collection<ClassLoaderNodeImpl> createNodes() throws MaterializationException {
        for (ClassLoaderNodeDefinition nodeDef : graphDefinition.getNodes()) {
            getOrCreateNode(nodeDef);
        }
        initImports();
        return nodes.values();
    }

    private ClassLoaderNodeImpl getOrCreateNode(ClassLoaderNodeDefinition nodeDef) throws MaterializationException {
        if (nodeDef == null) {
            return null;
        }
        ClassLoaderNodeImpl node = nodes.get(nodeDef.getName());
        if (node == null) {
            ClassLoaderNodeImpl parent = getOrCreateNode(nodeDef.getParent());
            ClassLoader parentCl = parent == null ? graphDefinition.getParentClassLoader() : parent.getClassLoader();
            URL[] sources = resolveSources(nodeDef);
            ClassLoadingStrategy loadingStrategy = nodeDef.getLoadingStrategy() == null ?
                    graphDefinition.getDefaultLoadingStrategy() : nodeDef.getLoadingStrategy();
            List<TargetImportItem> classImports = classImportsWithoutClassLoader(nodeDef);
            List<TargetImportItem> resourceImports = resourceImportsWithoutClassLoader(nodeDef);
            ClgClassLoader cl = new ClgClassLoader(sources, parentCl, loadingStrategy, classImports, resourceImports);
            node = new ClassLoaderNodeImpl(nodeDef.getName(), parent, cl);
            if (parent != null) {
                parent.registerChild(node);
            }
            nodes.put(nodeDef.getName(), node);
        }
        return node;
    }


    private List<TargetImportItem> classImportsWithoutClassLoader(ClassLoaderNodeDefinition nodeDef) {
        Collection<ImportDefinition> importClassesDef = nodeDef.getImportClasses();
        ArrayList<TargetImportItem> imports = new ArrayList<>(importClassesDef.size());
        for (ImportDefinition importDef : importClassesDef) {
            if (log.isTraceEnabled()) {
                log.trace("Node [{}] importing class(es) [{}] from node [{}]",
                        nodeDef.getName(), importDef.getPath(), importDef.getTarget().getName());
            }
            TargetImportItem importEntry = new TargetImportItem(null, createClassImport(importDef.getPath()));
            registerImportOnTarget(importDef.getTarget(), importEntry);
            imports.add(importEntry);
        }
        return imports;
    }

    private List<TargetImportItem> resourceImportsWithoutClassLoader(ClassLoaderNodeDefinition nodeDef) {
        Collection<ImportDefinition> importClassesDef = nodeDef.getImportResources();
        ArrayList<TargetImportItem> imports = new ArrayList<>(importClassesDef.size());
        for (ImportDefinition importDef : importClassesDef) {
            if (log.isTraceEnabled()) {
                log.trace("Node [{}] importing resource(s) [{}] from node [{}]",
                        nodeDef.getName(), importDef.getPath(), importDef.getTarget().getName());
            }
            TargetImportItem importEntry = new TargetImportItem(null, createResourceImport(importDef.getPath()));
            registerImportOnTarget(importDef.getTarget(), importEntry);
            imports.add(importEntry);
        }
        return imports;
    }

    private void registerImportOnTarget(ClassLoaderNodeDefinition target, TargetImportItem item) {
        List<TargetImportItem> targetImportItems = allImports.get(target.getName());
        if (targetImportItems == null) {
            targetImportItems = new ArrayList<>();
            allImports.put(target.getName(), targetImportItems);
        }
        targetImportItems.add(item);
    }

    private URL[] resolveSources(ClassLoaderNodeDefinition nodeDef) throws MaterializationException {
        List<URL> urls = new ArrayList<>();
        for (String source : nodeDef.getSources()) {
            UrlHolder holder = resolveSource(source);
            urls.addAll(holder.getURLs());
        }
        log.debug("Resolved source for node {}: {}", nodeDef.getName(), urls);
        return urls.toArray(new URL[0]);
    }

    private UrlHolder resolveSource(String source) throws MaterializationException {
        for (SourceResolver resolver : resolvers) {
            UrlHolder holder = resolver.resolveSource(source, classLoader);
            if (holder != null) {
                log.debug("Resolved {} to {} using resolver {}", source, holder, resolver);
                registerPossibleHandler(holder);
                return holder;
            }
        }
        throw new MaterializationException("Can't resolve source [" + source + "]");
    }

    private void registerPossibleHandler(Object object) {
        if (object instanceof ResourceHandler) {
            resourceHandlers.add((ResourceHandler) object);
            log.trace("Resource handler [{}] registered for closing", object);
        }
    }

    private void initImports() {
        for (Entry<String, List<TargetImportItem>> e : allImports.entrySet()) {
            ClassLoader nodeClassloader = nodes.get(e.getKey()).getClassLoader();
            for (TargetImportItem importItem : e.getValue()) {
                importItem.setTarget(nodeClassloader);
            }
        }
    }
}
