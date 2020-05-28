package io.github.alopukhov.dare.clg.impl;

public interface ValidationResult {
    boolean isSuccessful();

    void describe(StringBuilder to);
}
