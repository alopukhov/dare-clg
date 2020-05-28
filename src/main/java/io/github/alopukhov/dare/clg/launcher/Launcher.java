package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.ClassLoaderGraph;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Collections.singleton;

@Slf4j
@RequiredArgsConstructor
public class Launcher {
    public static final String LAUNCH_CONFIG_LOCATION = "dare.clg.launch.config.location";
    public static final String LAUNCH_CONFIG_READER_CLASS = "dare.clg.launch.config.reader";
    public static final String CONFIG_FILE_NAME = "clg-launch";
    private static final List<String> properties = new ArrayList<>();


    public static void main(String[] args) {
        //TODO
    }

    private void run(LaunchContext context) throws Exception {
        if (Launcher.class.getName().equals(context.mainClass())) {
            log.error("Launcher is configured to run itself as application entry point. This operation is forbidden");
            throw new LaunchException("Can't run launcher class");
        }
        ClassLoaderGraph graph = null;
        GraphAwareAppRunner target;
        try {
            graph = context.graph();
            ClassLoader mainClassloader = graph.getNode(context.mainNode()).getClassLoader();
            Class<?> mainClass = mainClassloader.loadClass(context.mainClass);
            if (GraphAwareAppRunner.class.isAssignableFrom(mainClass)) {
                log.debug("Instantiating ClassLoaderGraph aware runnable");
                target = ((GraphAwareAppRunner) mainClass.getConstructor().newInstance());
            } else {
                Method main = mainClass.getMethod("main", String[].class);
                boolean isPublicStatic = Modifier.isStatic(main.getModifiers()) && Modifier.isPublic(main.getModifiers());
                if (!isPublicStatic) {
                    log.error("Main method {} is not public static", main);
                    throw new LaunchException("Not public static main method");
                }
                target = new MainMethodRunner(main);
            }
        } catch (Exception e) {
            log.warn("Can't prepare load class", e);
            if (graph != null) {
                try {
                    graph.close();
                } catch (Exception closeException) {
                    e.addSuppressed(closeException);
                }
            }
            if (e instanceof LaunchException) {
                throw e;
            }
            throw new LaunchException(e);
        }
        target.run(graph, context.args());
    }

    private Collection<LaunchInfoReader> getInfoReaders() throws LaunchException {
        String className = System.getProperty(LAUNCH_CONFIG_READER_CLASS);
        if (className != null) {
            log.info("Configured to use class {} for reading config file", className);
            return singleton(getInfoReaderByClassName(className));
        }
        List<LaunchInfoReader> readers = new ArrayList<>(defaultReaders());
        for (LaunchInfoReader reader : ServiceLoader.load(LaunchInfoReader.class, Launcher.class.getClassLoader())) {
            readers.add(reader);
        }
        return readers;
    }

    private Collection<LaunchInfoReader> defaultReaders() {
        return Collections.<LaunchInfoReader>singletonList(new PropertiesLaunchInfoReader());
    }

    private LaunchInfoReader getInfoReaderByClassName(String className) throws LaunchException {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new LaunchException(e);
        }
        if (!LaunchInfoReader.class.isAssignableFrom(clazz)) {
            log.error("Configured class {} does not implement {} interface", clazz, LaunchInfoReader.class);
            throw new LaunchException("Bad config reader class");
        }
        try {
            return ((LaunchInfoReader) clazz.getConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            log.error("Can't instantiate class {}", clazz, e);
            throw new LaunchException("Can't instantiate reader class via reflection", e);
        }
    }

    @Accessors(fluent = true, chain = true)
    @Getter
    @Setter
    @RequiredArgsConstructor
    private static class LaunchContext {
        private ClassLoaderGraph graph;
        private String mainNode;
        private String mainClass;
        private String[] args;
    }

    @RequiredArgsConstructor
    private static class MainMethodRunner implements GraphAwareAppRunner {
        private final Method main;

        @Override
        public void run(ClassLoaderGraph graph, String[] args) throws Exception {
            main.invoke(null, new Object[]{args});
        }
    }
}
