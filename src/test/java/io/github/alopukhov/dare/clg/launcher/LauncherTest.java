package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.UnpackedTestJarsRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class LauncherTest {
    private static final String HELLO_WORLD_TEXT = "Dare to say 'hello world'?";

    @ClassRule
    public static final UnpackedTestJarsRule testJars = new UnpackedTestJarsRule();
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public final ClearSystemProperties clearLocationProperty = new ClearSystemProperties(Launcher.LAUNCH_CONFIG_LOCATION);

    @Test
    public void testHelloWorldDirectLocationSetup() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString().replace('\\', '/'));
        p.setProperty("launcher.main.class", "HelloWorld");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists().hasContent(HELLO_WORLD_TEXT);
    }

    @Test
    public void testHelloWorldSystemPropertyLocationSetup() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString().replace('\\', '/'));
        p.setProperty("launcher.main.class", "HelloWorld");
        Path propsLocation = saveProperties(p);
        System.setProperty(Launcher.LAUNCH_CONFIG_LOCATION, propsLocation.toString());
        Launcher launcher = new Launcher();
        launcher.readConfiguration();
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists().hasContent(HELLO_WORLD_TEXT);
    }

    @Test
    public void testHelloWorldWildcardSources() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.getRoot().toString() + "/*printer*.jar");
        p.setProperty("launcher.main.class", "HelloWorld");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists().hasContent(HELLO_WORLD_TEXT);
    }

    @Test
    public void testSummaryPrinterWithoutOtherJars() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString());
        p.setProperty("launcher.main.class", "SummaryPrinter");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists();
        assertThat(readFile(outputPath()))
                .contains("A: not found")
                .contains("B: not found")
                .contains("C: not found")
                .contains("Base: not found")
                .contains("Source: not found")
                .doesNotContain(": found");
    }

    @Test
    public void testSummaryPrinterWithAllJarsInSources() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.getRoot().toString() + "/*.jar");
        p.setProperty("launcher.main.class", "SummaryPrinter");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists();
        assertThat(readFile(outputPath()))
                .contains("A: found")
                .contains("B: found")
                .contains("C: found")
                .contains("Base: found")
                .contains("Source: found")
                .doesNotContain("not found");
    }

    @Test
    public void testSummaryPrinterWithImportJarB() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString());
        p.setProperty("node.jarb.sources", testJars.jarB().toString());
        p.setProperty("node.app.import.from.jarb.classes", "**");
        p.setProperty("launcher.main.class", "SummaryPrinter");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists();
        assertThat(readFile(outputPath()))
                .contains("A: not found")
                .contains("B: found; source: jar-b; super-source: jar-b")
                .contains("C: found; source: jar-b; super-source: jar-b")
                .contains("Base: found")
                .contains("Source: found");
    }

    @Test
    public void testSummaryPrinterWithTwoImports() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString());
        p.setProperty("node.jara.sources", testJars.jarA().toString());
        p.setProperty("node.jarb.sources", testJars.jarB().toString());
        p.setProperty("node.app.import.from.jara.classes", "A");
        p.setProperty("node.app.import.from.jarb.classes", "B,C");
        p.setProperty("launcher.main.class", "SummaryPrinter");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists();
        assertThat(readFile(outputPath()))
                .contains("A: found; source: jar-a; super-source: jar-a")
                .contains("B: found; source: jar-b; super-source: jar-b")
                .contains("C: found; source: jar-b; super-source: jar-b")
                .contains("Base: not found")
                .contains("Source: not found");
    }

    @Test
    public void testSummaryPrinterWithInheritance() throws Exception {
        // given
        Properties p = new Properties();
        p.setProperty("node.app.sources", testJars.testPrinterJar().toString());
        p.setProperty("node.jara.sources", testJars.jarA().toString());
        p.setProperty("node.jarb.sources", testJars.jarB().toString());
        p.setProperty("node.app.parent", "jara");
        p.setProperty("node.jara.parent", "jarb");
        p.setProperty("launcher.main.class", "SummaryPrinter");
        Path propsLocation = saveProperties(p);
        Launcher launcher = new Launcher();
        launcher.readConfiguration(propsLocation.toString());
        // when
        launcher.run(new String[]{outputPath().toString()});
        // then
        assertThat(outputPath()).exists();
        assertThat(readFile(outputPath()))
                .contains("A: found; source: jar-a; super-source: jar-b")
                .contains("B: found; source: jar-b; super-source: jar-b")
                .contains("C: found; source: jar-b; super-source: jar-b")
                .contains("Base: found")
                .contains("Source: found")
                .doesNotContain("not found");
    }

    private Path saveProperties(Properties p) throws IOException {
        Path path = tempFolder.getRoot().toPath().resolve("config.properties");
        try (BufferedWriter out = Files.newBufferedWriter(path, UTF_8)) {
            p.store(out, "test config");
        }
        return path;
    }

    private static String readFile(Path p) throws IOException {
        return new String(Files.readAllBytes(p), UTF_8);
    }

    private Path outputPath() {
        return tempFolder.getRoot().toPath().resolve("output.txt");
    }
}