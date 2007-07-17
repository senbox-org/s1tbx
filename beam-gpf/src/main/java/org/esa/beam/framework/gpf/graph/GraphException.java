package org.esa.beam.framework.gpf.graph;


/**
 * A general exception class for all failures that occure
 * during the use and execution of {@link Graph}s.
 *
 * @author Maximilian Aulinger
 */
public class GraphException extends Exception {

    public GraphException(String message) {
        super(message);
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
