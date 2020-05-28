package io.github.alopukhov.dare.clg;

public class MaterializationException extends Exception {
    public MaterializationException() {
    }

    public MaterializationException(String message) {
        super(message);
    }

    public MaterializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaterializationException(Throwable cause) {
        super(cause);
    }
}
