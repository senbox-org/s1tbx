package com.bc.ceres.binding;

/**
 * Signals a value validation failure.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);

    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
