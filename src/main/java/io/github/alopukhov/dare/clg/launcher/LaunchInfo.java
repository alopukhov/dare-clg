package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.ClassLoaderGraphDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LaunchInfo {
    private ClassLoaderGraphDefinition graphDefinition;
    private String applicationNodeName = "app";
    private String mainClass;
}