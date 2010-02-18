package org.esa.beam.framework.gpf.graph;


/**
 * A general exception class for all failures that occurs
 * during the use and execution of {@link Graph}s.
 *
 * @author Maximilian Aulinger
 */
public class GraphException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public GraphException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
