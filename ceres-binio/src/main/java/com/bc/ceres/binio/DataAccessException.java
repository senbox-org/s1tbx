package com.bc.ceres.binio;

/**
 * A runtime exception which is used to signal an illegal data access throughout the API.
 */
public class DataAccessException extends RuntimeException {
    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public DataAccessException() {
        super("Illegal data access.");
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public DataAccessException(String message) {
        super(message);
    }
}
