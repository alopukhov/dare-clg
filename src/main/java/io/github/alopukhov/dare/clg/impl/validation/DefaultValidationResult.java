package io.github.alopukhov.dare.clg.impl.validation;

import io.github.alopukhov.dare.clg.impl.ValidationResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DefaultValidationResult implements ValidationResult {
    private final String testName;
    private final boolean successful;

    public static ValidationResult success(String testName) {
        return new DefaultValidationResult(testName, true);
    }

    public static ValidationResult failure(String testName) {
        return new DefaultValidationResult(testName, false);
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public void describe(StringBuilder to) {
        to.append(testName)
                .append(": validation ")
                .append(isSuccessful() ? "passed" : "failed");
        describeDetails(to, " ");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        describe(builder);
        return builder.toString();
    }

    protected void describeDetails(StringBuilder to, String indent) {
    }
}
