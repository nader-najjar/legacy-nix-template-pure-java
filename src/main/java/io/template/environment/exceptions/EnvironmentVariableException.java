package io.template.environment.exceptions;

/**
 * Thrown when a required environment variable is missing or invalid.
 */
public final class EnvironmentVariableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EnvironmentVariableException(String message) {
        super(message);
    }

    public EnvironmentVariableException(String message, Throwable cause) {
        super(message, cause);
    }
}
