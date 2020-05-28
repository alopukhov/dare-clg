package io.github.alopukhov.dare.clg.launcher;

import java.io.InputStream;
import java.util.Collection;

import static java.util.Collections.singleton;

public class PropertiesLaunchInfoReader implements LaunchInfoReader {
    @Override
    public Collection<String> supportedFileExtensions() {
        return singleton(".properties");
    }

    @Override
    public LaunchInfo readLaunchInfo(InputStream inputStream) {
        return null;
    }
}
