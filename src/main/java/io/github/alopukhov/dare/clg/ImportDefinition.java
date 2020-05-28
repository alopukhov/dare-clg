package io.github.alopukhov.dare.clg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ImportDefinition {
    private final ClassLoaderNodeDefinition target;
    private final String path;
}
