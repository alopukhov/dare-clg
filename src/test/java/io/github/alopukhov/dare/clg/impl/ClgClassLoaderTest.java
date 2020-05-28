package io.github.alopukhov.dare.clg.impl;

import lombok.Cleanup;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static io.github.alopukhov.dare.clg.impl.ImportItem.createClassImport;
import static io.github.alopukhov.dare.clg.impl.ReflectUtils.newInstance;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ClgClassLoaderTest {
    @ClassRule
    public static final UnpackedTestJarsRule jars = new UnpackedTestJarsRule();

    @Test
    public void simpleImportTest() throws Exception {
        @Cleanup URLClassLoader a = urlClassLoader(jars.jarUrlA());
        @Cleanup URLClassLoader b = urlClassLoader(jars.jarUrlB());
        @Cleanup URLClassLoader c = urlClassLoader(jars.jarUrlC());
        @Cleanup ClgClassLoader cl = new ClgClassLoader(new URL[0], ClassLoader.getSystemClassLoader(),
                BaseStrategies.PIS, asList(
                createImport(a, "A"),
                createImport(b, "B"),
                createImport(c, "C")),
                Collections.<TargetImportItem>emptyList());
        Object A = newInstance(cl, "A");
        Object B = newInstance(cl, "B");
        Object C = newInstance(cl, "C");
        assertThat(A.getClass().getClassLoader()).isEqualTo(a);
        assertThat(B.getClass().getClassLoader()).isEqualTo(b);
        assertThat(C.getClass().getClassLoader()).isEqualTo(c);
        assertThat(asList(A, B, C)).extracting("source").containsExactly("jar-a", "jar-b", "jar-c");
        assertThat(asList(A, B, C)).extracting("superSource").containsExactly("jar-a", "jar-b", "jar-c");
    }

    private static TargetImportItem createImport(ClassLoader from, String importPath) {
        return new TargetImportItem(from, createClassImport(importPath));
    }

    private URLClassLoader urlClassLoader(URL... urls) {
        return new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
    }
}