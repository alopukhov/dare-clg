package io.github.alopukhov.dare.clg.impl;

import io.github.alopukhov.dare.clg.spi.SimpleUrlHolder;
import io.github.alopukhov.dare.clg.spi.SourceResolver;
import io.github.alopukhov.dare.clg.spi.UrlHolder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
class DefaultSourceResolver implements SourceResolver {
    private static final List<String> SIMPLE_PROTOCOLS = Arrays.asList("http:", "https:", "file:");
    private static final String CLASSPATH_PROTOCOL = "classpath:";

    @Override
    public UrlHolder resolveSource(String sourcePath, ClassLoader classLoader) {
        if (sourcePath.isEmpty()) {
            return null;
        }
        UrlHolder urls = resolveAsFile(sourcePath);
        if (urls == null) {
            urls = resolveAsSimpleUrl(sourcePath);
        }
        if (urls == null) {
            urls = resolveAsClasspathDir(sourcePath, classLoader);
        }
        return urls;
    }

    private static UrlHolder resolveAsFile(String sourcePath) {
        if (!(sourcePath.charAt(0) == '/' || sourcePath.indexOf(':') < 0)) {
            return null;
        }
        try {
            int dirPos = sourcePath.lastIndexOf('/');
            String dir = (dirPos < 0) ? "." : sourcePath.substring(0, dirPos + 1);
            String file = (dirPos < 0) ? sourcePath : sourcePath.substring(dirPos + 1);
            Path dirPath = dir.startsWith("~/") ?
                    Paths.get(System.getProperty("user.home")).resolve(dir.replaceFirst("~/+", ""))
                    : Paths.get(dir);
            if (file.isEmpty()) {
                URL url = asURL(Paths.get(dir));
                String urlString = url.toString();
                if (urlString.charAt(urlString.length() - 1) != '/') {
                    try {
                        url = new URL(urlString + '/');
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException("Can't append / to url", e);
                    }
                }
                return SimpleUrlHolder.create(url);
            }
            if (file.indexOf('*') < 0) {
                return SimpleUrlHolder.create(asURL(dirPath.resolve(file)));
            }
            Pattern filePattern = getFilenamePattern(file);
            FilesCollector filesCollector = new FilesCollector(filePattern);
            Files.walkFileTree(dirPath, filesCollector);
            return SimpleUrlHolder.create(filesCollector.getResult());
        } catch (InvalidPathException e) {
            log.warn("Invalid filesystem path for source path [{}]", sourcePath, e);
            return null;
        } catch (IOException e) {
            log.warn("IO exception occured during attempt to resolve source [{}]", sourcePath, e);
            return null;
        }
    }

    private static UrlHolder resolveAsSimpleUrl(String sourcePath) {
        if (canUseUrlConstructor(sourcePath)) {
            try {
                return SimpleUrlHolder.create(new URL(sourcePath));
            } catch (MalformedURLException malformedURLException) {
                log.debug("Source path [{}] can't be resolved via direct URL instantiation", sourcePath);
            }
        }
        return null;
    }

    private static UrlHolder resolveAsClasspathDir(String sourcePath, ClassLoader classLoader) {
        if (sourcePath.startsWith(CLASSPATH_PROTOCOL) && sourcePath.endsWith("/") && !sourcePath.contains("!/")) {
            String resourcePath = sourcePath.substring(CLASSPATH_PROTOCOL.length());
            URL dirUrl = classLoader.getResource(resourcePath);
            if (dirUrl != null) {
                return SimpleUrlHolder.create(dirUrl);
            }
        }
        return null;
    }

    private static Pattern getFilenamePattern(String file) {
        StringBuilder filePatternString = new StringBuilder(file.length() + 10);
        for (String part : file.split("\\*")) {
            if (filePatternString.length() > 0) {
                filePatternString.append(".*");
            }
            filePatternString.append(Pattern.quote(part));
        }
        return Pattern.compile(filePatternString.toString());
    }

    private static boolean canUseUrlConstructor(String sourcePath) {
        for (String protocol : SIMPLE_PROTOCOLS) {
            if (sourcePath.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }

    private static URL asURL(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            log.error("Can't convert path [{}] to URL", p, e);
            throw new IllegalStateException("Can't convert valid Path to URL", e);
        }
    }

    @RequiredArgsConstructor
    private static class FilesCollector implements FileVisitor<Path> {
        private final Pattern includePattern;
        @Getter(AccessLevel.PRIVATE)
        private final List<URL> result = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (!attrs.isDirectory() && includePattern.matcher(file.getFileName().toString()).matches()){
                result.add(asURL(file));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
