package com.bc.ceres.binding;

/**
 * Signals a binding failure.
 *
 * @author Norman Fomferra
 * @since 0.10
 */
public class BindingException extends Exception {
    public BindingException(String message) {
        super(message);

    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }
}