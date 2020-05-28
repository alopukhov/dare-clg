package io.github.alopukhov.dare.clg.launcher;

public class LaunchException extends Exception {
    LaunchException(String message) {
        super(message);
    }

    LaunchException(Throwable cause) {
        super(cause);
    }

    LaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
