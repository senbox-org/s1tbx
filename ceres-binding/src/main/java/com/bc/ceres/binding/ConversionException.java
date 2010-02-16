package com.bc.ceres.binding;


/**
 * Signals a value conversion failure.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ConversionException extends BindingException {
    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(Throwable cause) {
        this(cause.getMessage(), cause);
    }
}
