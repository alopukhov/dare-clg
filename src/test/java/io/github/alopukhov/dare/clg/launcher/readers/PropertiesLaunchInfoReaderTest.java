package io.github.alopukhov.dare.clg.launcher.readers;

import io.github.alopukhov.dare.clg.ClassLoaderGraphDefinition;
import io.github.alopukhov.dare.clg.launcher.ConfigurationException;
import io.github.alopukhov.dare.clg.launcher.LaunchInfo;
import io.github.alopukhov.dare.clg.launcher.LaunchInfoReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesLaunchInfoReaderTest {
    @Test
    public void supportsSpacesBetweenArrayElements() throws IOException, ConfigurationException {
        String props = "node.a.import.from.b.classes = a, b , c ,d \n" +
                "node.a.import.from.b.resources : e, f , g ,h \n" +
                "node.a.sources = i, j , k ,l \n\n";
        InputStream propsInputStream = inputStreamFromString(props);
        LaunchInfoReader infoReader = new PropertiesLaunchInfoReader();
        LaunchInfo launchInfo = infoReader.readLaunchInfo(propsInputStream);
        ClassLoaderGraphDefinition graphDefinition = launchInfo.getGraphDefinition();
        assertThat(graphDefinition.getNodes()).extracting("name").containsExactlyInAnyOrder("a", "b");
        assertThat(graphDefinition.getNode("a").getImportClasses()).extracting("path")
                .containsExactly("a", "b", "c", "d");
        assertThat(graphDefinition.getNode("a").getImportResources()).extracting("path")
                .containsExactly("e", "f", "g", "h");
        assertThat(graphDefinition.getNode("a").getSources()).containsExactly("i", "j", "k", "l");
    }

    private InputStream inputStreamFromString(String string) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(string.getBytes(StandardCharsets.UTF_8));
        return new ByteArrayInputStream(baos.toByteArray());
    }
}