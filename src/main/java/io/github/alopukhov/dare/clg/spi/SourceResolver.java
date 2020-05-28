package io.github.alopukhov.dare.clg.spi;

public interface SourceResolver {
    UrlHolder resolveSource(String sourcePath, ClassLoader classLoader);
}
