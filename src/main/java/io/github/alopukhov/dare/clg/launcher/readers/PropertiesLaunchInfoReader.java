package io.github.alopukhov.dare.clg.launcher.readers;

import io.github.alopukhov.dare.clg.launcher.ConfigurationException;
import io.github.alopukhov.dare.clg.launcher.LaunchInfo;
import io.github.alopukhov.dare.clg.launcher.LaunchInfoReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.alopukhov.dare.clg.ClassloaderGraphFactory.defineNewGraph;
import static io.github.alopukhov.dare.clg.launcher.ClassLoaderUtil.byName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * TODO: add property order guarantee
 */
@Slf4j
public class PropertiesLaunchInfoReader implements LaunchInfoReader {
    @Override
    public Collection<String> supportedFileExtensions() {
        return singleton(".properties");
    }

    @Override
    public LaunchInfo readLaunchInfo(InputStream inputStream) throws IOException, ConfigurationException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(inputStream, UTF_8));
        PropertiesHandler handler = new PropertiesHandler();
        handler.handle(properties);
        return handler.info;
    }

    private static class PropertiesHandler {
        private final LaunchInfo info;

        public PropertiesHandler() {
            info = new LaunchInfo();
            info.setGraphDefinition(defineNewGraph());
        }

        public void handle(Properties properties) throws ConfigurationException {
            drainStaticProperties(properties);
            handleDynamicProperties(properties);
        }

        private void drainStaticProperties(Properties properties) throws ConfigurationException {
            for (StaticProperty prop : StaticProperty.values()) {
                prop.drain(properties, info);
            }
        }

        private void handleDynamicProperties(Properties properties) throws ConfigurationException {
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                String key = (String) prop.getKey();
                String value = (String) prop.getValue();
                DynamicProperty.handleOrThrow(key, value, info);
            }
        }
    }

    @RequiredArgsConstructor
    enum DynamicProperty {
        IMPORT_CLASSES(Pattern.compile("^node\\.([^.]+)\\.import\\.from\\.([^.]+)\\.classes$")) {
            @Override
            protected void doHandle(Matcher keyMatcher, String value, LaunchInfo target) {
                String targetNode = keyMatcher.group(1);
                String fromNode = keyMatcher.group(2);
                for (String classes : value.split(",")) {
                    log.debug("Importing classes to node [{}] from [{}]: [{}]", targetNode, fromNode, classes);
                    target.getGraphDefinition().getOrCreateNode(targetNode).addImportClasses(fromNode, classes);
                }
            }
        },
        IMPORT_RESOURCES(Pattern.compile("^node\\.([^.]+)\\.import\\.from\\.([^.]+)\\.resources$")) {
            @Override
            protected void doHandle(Matcher keyMatcher, String value, LaunchInfo target) {
                String targetNode = keyMatcher.group(1);
                String fromNode = keyMatcher.group(2);
                for (String resources : value.split(",")) {
                    log.debug("Importing resources to node [{}] from [{}]: [{}]", targetNode, fromNode, resources);
                    target.getGraphDefinition().getOrCreateNode(targetNode).addImportClasses(fromNode, resources);
                }
            }
        },
        SET_PARENT(Pattern.compile("^node\\.([^.]+)\\.parent$")) {
            @Override
            protected void doHandle(Matcher keyMatcher, String value, LaunchInfo target) {
                String node = keyMatcher.group(1);
                log.debug("Setting node [{}] parent to [{}]", node, value);
                target.getGraphDefinition().getOrCreateNode(node).setParent(value);
            }
        },
        SET_LOADING_STRATEGY(Pattern.compile("^node\\.([^.]+)\\.loading\\.strategy$")) {
            @Override
            protected void doHandle(Matcher keyMatcher, String value, LaunchInfo target) {
                String node = keyMatcher.group(1);
                log.debug("Setting node [{}] loading strategy to [{}]", node, value);
                target.getGraphDefinition().getOrCreateNode(node).setLoadingStrategy(value);
            }
        },
        SET_SOURCES(Pattern.compile("^node\\.([^.]+)\\.sources$")) {
            @Override
            protected void doHandle(Matcher keyMatcher, String value, LaunchInfo target) {
                String node = keyMatcher.group(1);
                List<String> paths = asList(value.split(","));
                log.debug("Adding node [{}] sources: {}", node, paths);
                target.getGraphDefinition().getOrCreateNode(node).addSource(paths);
            }
        };

        private final Pattern pattern;

        public static void handleOrThrow(String key, String value, LaunchInfo target) throws ConfigurationException {
            for (DynamicProperty property : values()) {
                if (property.handle(key, value, target)) {
                    return;
                }
            }
            throw new ConfigurationException("Unknown property [" + key + "]");
        }

        private boolean handle(String key, String value, LaunchInfo target) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                doHandle(matcher, value, target);
                return true;
            }
            return false;
        }

        protected abstract void doHandle(Matcher keyMatcher, String value, LaunchInfo target);
    }


    @RequiredArgsConstructor
    enum StaticProperty {
        MAIN_CLASS("launcher.main.class") {
            @Override
            public void set(LaunchInfo target, String value) {
                target.setMainClassname(value.trim());
            }
        },
        MAIN_NODE("launcher.main.node") {
            @Override
            public void set(LaunchInfo target, String value) {
                target.setMainNode(value);
            }
        },
        MATERIALIZER_CLASSLOADER("launcher.materializer.classloader") {
            @Override
            public void set(LaunchInfo target, String value) {
                target.setMaterializerClassLoader(byName(value));
            }
        },
        GRAPH_DEFAULT_LOADING("graph.default.loading.strategy") {
            @Override
            public void set(LaunchInfo target, String value) {
                target.getGraphDefinition().setDefaultLoadingStrategy(value);
            }
        },
        GRAPH_PARENT_CLASSLOADER("graph.parent.classloader") {
            @Override
            public void set(LaunchInfo target, String value) {
                target.getGraphDefinition().setParentClassLoader(byName(value));
            }
        };

        private final String key;

        public void drain(Properties properties, LaunchInfo target) throws ConfigurationException {
            String value = properties.getProperty(key);
            if (value == null) {
                return;
            }
            properties.remove(key);
            try {
                set(target, value);
            } catch (Exception e) {
                log.error("Can't set property [{}={}]", key, value, e);
                throw new ConfigurationException(e);
            }
        }

        public abstract void set(LaunchInfo target, String value);
    }
}
