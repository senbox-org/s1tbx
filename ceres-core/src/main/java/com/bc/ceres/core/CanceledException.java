package com.bc.ceres.core;

/**
 * An exception which clients can use to signal that a {@link ProgressMonitor} requested cancelation
 * of a running process.
 */
public class CanceledException extends CoreException {
    /**
     * Constructs a new exception.
     */
    public CanceledException() {
        super("Canceled by user request.");
    }
}
