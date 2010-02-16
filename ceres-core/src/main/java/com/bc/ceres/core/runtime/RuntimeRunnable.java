package com.bc.ceres.core.runtime;

import com.bc.ceres.core.ProgressMonitor;

/**
 * A runnable which is executed by the runtime.
 * <p/>
 * This interface may be implemented by clients.</p>
 */
public interface RuntimeRunnable {

    /**
     * Executes client code.
     * <p/>
     * If this RuntimeRunnable is an application launched by the Ceres runtime,
     * the <code>argument</code> parameter can safely be casted to a <code>String[]</code>.
     * This array contains all command-line arguments passed to the application.
     *
     * @param argument the argument passed to the RuntimeRunnable, which may be null.
     * @param pm a progress monitor which may be used by the client code
     * @throws Exception if any error occurs in the client code
     */
    void run(Object argument, ProgressMonitor pm) throws Exception;
}
