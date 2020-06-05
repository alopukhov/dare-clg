package io.github.alopukhov.dare.clg;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Accessors(fluent = true)
@Getter
public class UnpackedTestJarsRule extends TemporaryFolder {

    private Path jarA;
    private Path jarB;
    private Path jarC;
    private Path testPrinterJar;
    private URL jarUrlA;
    private URL jarUrlB;
    private URL jarUrlC;
    private URL testPrinterJarUrl;

    @Override
    protected void before() throws Throwable {
        super.before();
        jarA = copyToJarsFolder(requireNonNull(resource("jar-a.jar")));
        jarUrlA = asUrl(jarA);
        jarB = copyToJarsFolder(requireNonNull(resource("jar-b.jar")));
        jarUrlB = asUrl(jarB);
        jarC = copyToJarsFolder(requireNonNull(resource("jar-c.jar")));
        jarUrlC = asUrl(jarC);
        testPrinterJar = copyToJarsFolder(requireNonNull(resource("test-printer.jar")));
        testPrinterJarUrl = asUrl(testPrinterJar);
    }

    @Override
    protected void after() {
        jarA = null;
        jarB = null;
        jarC = null;
        testPrinterJar = null;
        jarUrlA = null;
        jarUrlB = null;
        jarUrlC = null;
        testPrinterJarUrl = null;
        super.after();
    }


    private Path copyToJarsFolder(URL url) {
        try {
            String urlString = url.toString();
            String name = urlString.substring(urlString.lastIndexOf('/') + 1);
            Path target = new File(getRoot(), name).toPath();
            System.out.printf("Copy %s -> %s\n", url, target);
            try (InputStream in = url.openStream()) {
                Files.copy(in, target);
            }
            return target;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public URL asUrl(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private URL resource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}
