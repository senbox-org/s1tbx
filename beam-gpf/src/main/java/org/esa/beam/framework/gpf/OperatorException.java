package org.esa.beam.framework.gpf;

/**
 * A general exception class for all failures within an {@link org.esa.beam.framework.gpf.Operator}
 * implementation.
 *
 * @author Maximilian Aulinger
 */
public class OperatorException extends Exception {

    public OperatorException(String message) {
        super(message);
    }

    public OperatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperatorException(Throwable cause) {
        this(cause.getMessage(), cause);
    }
}
