package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.ClassLoaderGraphDefinition;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class LaunchInfo {
    private ClassLoaderGraphDefinition graphDefinition;
    @NonNull
    private String mainNode = "app";
    private String mainClassname;
    @NonNull
    private ClassLoader materializerClassLoader = LaunchInfo.class.getClassLoader();
}