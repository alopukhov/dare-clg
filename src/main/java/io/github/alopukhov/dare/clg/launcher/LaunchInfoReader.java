package io.github.alopukhov.dare.clg.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface LaunchInfoReader {
    Collection<String> supportedFileExtensions();
    LaunchInfo readLaunchInfo(InputStream inputStream) throws IOException, ConfigurationException;
}
