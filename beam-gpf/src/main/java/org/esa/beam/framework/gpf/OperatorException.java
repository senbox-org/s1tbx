package org.esa.beam.framework.gpf;

/**
 * A general exception class for all failures within an {@link org.esa.beam.framework.gpf.Operator}
 * implementation.
 */
public class OperatorException extends RuntimeException {


    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public OperatorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public OperatorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception by delegating to
     * {@link #OperatorException(String,Throwable) this(cause.getMessage(), cause)}.
     *
     * @param cause the cause
     */
    public OperatorException(Throwable cause) {
        this(cause.getMessage(), cause);
    }
}
