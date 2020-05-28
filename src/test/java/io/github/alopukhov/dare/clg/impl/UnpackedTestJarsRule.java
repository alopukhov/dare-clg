package io.github.alopukhov.dare.clg.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Accessors(fluent = true)
public class UnpackedTestJarsRule extends TemporaryFolder {
    @Getter
    private URL jarUrlA;
    @Getter
    private URL jarUrlB;
    @Getter
    private URL jarUrlC;

    @Override
    protected void before() throws Throwable {
        super.before();
        jarUrlA = copyToJarsFolder(requireNonNull(resource("jar-a.jar")));
        jarUrlB = copyToJarsFolder(requireNonNull(resource("jar-b.jar")));
        jarUrlC = copyToJarsFolder(requireNonNull(resource("jar-c.jar")));
    }

    @Override
    protected void after() {
        jarUrlA = null;
        jarUrlB = null;
        jarUrlC = null;
        super.after();
    }


    private URL copyToJarsFolder(URL url) {
        try {
            String urlString = url.toString();
            String name = urlString.substring(urlString.lastIndexOf('/') + 1);
            Path target = new File(getRoot(), name).toPath();
            System.out.printf("Copy %s -> %s\n", url, target);
            try (InputStream in = url.openStream()) {
                Files.copy(in, target);
            }
            return target.toUri().toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private URL resource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}
