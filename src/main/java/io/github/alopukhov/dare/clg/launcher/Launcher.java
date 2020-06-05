package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.ClassLoaderGraph;
import io.github.alopukhov.dare.clg.MaterializationException;
import io.github.alopukhov.dare.clg.launcher.readers.PropertiesLaunchInfoReader;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Arrays.copyOfRange;
import static java.util.Objects.requireNonNull;

@Slf4j
@Accessors(fluent = true, chain = true)
@Getter
@Setter
public class Launcher {
    public static final String LAUNCH_CONFIG_LOCATION = "dare.clg.launch.config.location";
    public static final String CONFIG_FILE_NAME = "clg";
    public static final String CONFIG_DIRECTORY = "config";
    private static final String CLASSPATH_PREFIX = "classpath:";

    private List<String> searchClasspathLocations = Arrays.asList(CONFIG_DIRECTORY + "/", "");
    private List<Path> searchFilesystemLocations = Arrays.asList(
            Paths.get(".", CONFIG_DIRECTORY),
            Paths.get("."));

    @NonNull
    private ClassLoader configResolverClassloader = Launcher.class.getClassLoader();
    private LaunchInfo launchInfo;

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.readConfiguration();
        launcher.run(args);
    }

    public void readConfiguration() throws ConfigurationException {
        String location = System.getProperty(LAUNCH_CONFIG_LOCATION);
        if (location != null) {
            log.info("Config location path is set to [{}] via system property", location);
        }
        if (location == null) {
            List<String> defaultExtensions = new ArrayList<>();
            for (LaunchInfoReader reader : defaultReaders()) {
                defaultExtensions.addAll(reader.supportedFileExtensions());
            }
            log.debug("Looking for config file using built in config readers extensions. Associated file extensions (in order): {}", defaultExtensions);
            location = findConfiguration(defaultExtensions);
        }
        if (location == null) {
            List<String> extensions = new ArrayList<>();
            for (LaunchInfoReader reader : loadInfoReaders()) {
                extensions.addAll(reader.supportedFileExtensions());
            }
            log.debug("Looking for config file using config readers services. Associated file extensions (in order): {}", extensions);
            location = findConfiguration(extensions);
        }
        readConfiguration(location);
    }

    public void readConfiguration(String location) throws ConfigurationException {
        LaunchInfoReader reader = resolveReader(location);
        try (InputStream inputStream = openConfigFile(location)) {
            launchInfo = reader.readLaunchInfo(inputStream);
        } catch (Exception e) {
            log.error("Error reading config file", e);
            throw new ConfigurationException(e);
        }
    }

    public void run(String[] args) throws ConfigurationException, LaunchException {
        validateConfiguration(args);
        final String mainClassName;
        final String[] runArgs;
        if (launchInfo.getMainClassname() == null) {
            log.info("Main class name is unset. Using first parameter ({}) from run args as classname", args[0]);
            mainClassName = args[0];
            runArgs = copyOfRange(args, 1, args.length);
        } else {
            mainClassName = launchInfo.getMainClassname();
            runArgs = args;
        }
        if (Launcher.class.getName().equals(mainClassName)) {
            log.error("Launcher is configured to run itself as application entry point. This operation is forbidden");
            throw new ConfigurationException("Running class [" + mainClassName + "] is forbidden");
        }

        ClassLoaderGraph graph;
        try {
            graph = launchInfo.getGraphDefinition().materialize(launchInfo.getMaterializerClassLoader());
        } catch (MaterializationException e) {
            log.error("Can't materialize graph", e);
            throw new ConfigurationException("Can't materialize graph", e);
        }
        GraphAwareAppRunner appRunner;
        try {
            appRunner = createAppRunner(graph, launchInfo.getMainNode(), mainClassName);
        } catch (Exception e) {
            log.error("Can't create app runner. Closing graph");
            try {
                graph.close();
            } catch (Exception closeException) {
                log.error("Can't close graph properly", closeException);
                e.addSuppressed(closeException);
            }
            throw e;
        }
        try {
            appRunner.run(graph, runArgs);
        } catch (Exception e) {
            throw new LaunchException(e);
        }
    }

    private void validateConfiguration(String[] args) throws ConfigurationException {
        if (launchInfo == null) {
            throw new IllegalStateException("Not configured");
        }
        if (args.length == 0 && launchInfo.getMainClassname() == null) {
            log.error("Main class is unset and is not provided via arg");
            throw new ConfigurationException("Main class unspecified");
        }
        if (launchInfo.getGraphDefinition().getNode(launchInfo.getMainNode()) == null) {
            log.error("Graph definition does not contain main node [{}]", launchInfo.getMainNode());
            throw new ConfigurationException("Main node not defined");
        }
    }

    private static GraphAwareAppRunner createAppRunner(ClassLoaderGraph graph, String node, String mainClassname) throws ConfigurationException {
        try {
            ClassLoader mainClassloader = requireNonNull(graph.getNode(node).getClassLoader(), "Application graph node is not defined");
            Class<?> mainClass = mainClassloader.loadClass(mainClassname);
            if (GraphAwareAppRunner.class.isAssignableFrom(mainClass)) {
                log.info("Instantiating ClassLoaderGraph aware app runner {}", mainClass);
                return ((GraphAwareAppRunner) mainClass.getConstructor().newInstance());
            } else {
                Method main = mainClass.getMethod("main", String[].class);
                boolean isPublicStatic = Modifier.isStatic(main.getModifiers()) && Modifier.isPublic(main.getModifiers());
                if (!isPublicStatic) {
                    log.error("Main method {} is not public static", main);
                    throw new ConfigurationException("Not public static main method in class " + mainClass.getName());
                }
                return new MainMethodRunner(main);
            }
        } catch (Exception e) {
            log.warn("Can't prepare load class", e);
            if (e instanceof ConfigurationException) {
                throw (ConfigurationException) e;
            }
            throw new ConfigurationException(e);
        }
    }

    private String findConfiguration(Collection<String> extensions) {
        String location = findConfigurationInFilesystem(extensions);
        if (location == null) {
            location = findConfigurationInClasspath(extensions);
        }
        return location;
    }

    private String findConfigurationInFilesystem(Collection<String> extensions) {
        log.info("Looking for file {} with extensions {} in classpath. Search locations: {}", CONFIG_FILE_NAME, extensions, searchFilesystemLocations);
        for (Path directory : searchFilesystemLocations) {
            for (String extension : extensions) {
                Path configPath = directory.resolve(CONFIG_FILE_NAME + extension);
                if (configPath.toFile().exists()) {
                    log.debug("Found config [{}]", configPath);
                    return configPath.toString();
                }
            }
        }
        return null;
    }

    private String findConfigurationInClasspath(Collection<String> extensions) {
        log.info("Looking for file {} with extensions {} in classpath. Search locations: {}", CONFIG_FILE_NAME, extensions, searchClasspathLocations);
        for (String directory : searchClasspathLocations) {
            for (String extension : extensions) {
                String name = directory + CONFIG_FILE_NAME + extension;
                if (configResolverClassloader.getResource(name) != null) {
                    log.debug("Found config [{}] in classpath", name);
                    return CLASSPATH_PREFIX + name;
                }
            }
        }
        return null;
    }

    private InputStream openConfigFile(String location) throws ConfigurationException {
        InputStream inputStream;
        if (location.startsWith(CLASSPATH_PREFIX)) {
            location = location.substring(CLASSPATH_PREFIX.length());
            if (location.startsWith("/")) {
                log.trace("Removing leading '/' in classpath location [{}]", location);
                location = location.substring(1);
            }
            inputStream = configResolverClassloader.getResourceAsStream(location);
            if (inputStream == null) {
                log.error("File [{}] does not found in classpath", location);
                throw new ConfigurationException("Can't find config file in classpath");
            }
        } else {
            try {
                inputStream = Files.newInputStream(Paths.get(location));
            } catch (Exception e) {
                log.error("Can't open config file [{}]", location);
                throw new ConfigurationException("Can't open config file", e);
            }
        }
        return inputStream;
    }

    private List<LaunchInfoReader> loadInfoReaders() {
        List<LaunchInfoReader> readers = new ArrayList<>();
        for (LaunchInfoReader reader : ServiceLoader.load(LaunchInfoReader.class, configResolverClassloader)) {
            readers.add(reader);
        }
        return readers;
    }

    private static List<LaunchInfoReader> defaultReaders() {
        return Collections.<LaunchInfoReader>singletonList(new PropertiesLaunchInfoReader());
    }

    private LaunchInfoReader resolveReader(String location) throws ConfigurationException {
        for (LaunchInfoReader reader : defaultReaders()) {
            for (String extension : reader.supportedFileExtensions()) {
                if (location.endsWith(extension)) {
                    log.info("Config [{}] will be read using builtin reader", location);
                    return reader;
                }
            }
        }
        for (LaunchInfoReader reader : loadInfoReaders()) {
            for (String extension : reader.supportedFileExtensions()) {
                if (location.endsWith(extension)) {
                    log.info("Config [{}] will be read using reader service [{}]", location, reader.getClass());
                    return reader;
                }
            }
        }
        throw new ConfigurationException("Can't resolve config file reader");
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
