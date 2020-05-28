package io.github.alopukhov.dare.clg.launcher;

import io.github.alopukhov.dare.clg.ClassLoaderGraph;

public interface GraphAwareAppRunner {
    void run(ClassLoaderGraph graph, String[] args) throws Exception;
}
