package com.bc.ceres.core;

/**
 * An abstract wrapper around a progress monitor which,
 * unless overridden, forwards <code>IProgressMonitor</code>
 * and <code>IProgressMonitorWithBlocking</code> methods to the wrapped progress monitor.
 * <p>
 * Clients may subclass.
 * </p>
 * <p>This class has been more or less directly taken over from the <a href="http://www.eclipse.org/">Eclipse</a> Core API.</p>
 */
public abstract class ProgressMonitorWrapper implements ProgressMonitor {

    /**
     * The wrapped progress monitor.
     */
    private ProgressMonitor progressMonitor;

    /**
     * Creates a new wrapper around the given monitor.
     *
     * @param monitor the progress monitor to forward to
     */
    protected ProgressMonitorWrapper(ProgressMonitor monitor) {
        Assert.notNull(monitor);
        progressMonitor = monitor;
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#beginTask(String, int)
     */
    public void beginTask(String taskName, int totalWork) {
        progressMonitor.beginTask(taskName, totalWork);
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#done()
     */
    public void done() {
        progressMonitor.done();
    }

    /**
     * Returns the wrapped progress monitor.
     *
     * @return the wrapped progress monitor
     */
    public ProgressMonitor getWrappedProgressMonitor() {
        return progressMonitor;
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#internalWorked(double)
     */
    public void internalWorked(double work) {
        progressMonitor.internalWorked(work);
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#isCanceled()
     */
    public boolean isCanceled() {
        return progressMonitor.isCanceled();
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#setCanceled(boolean)
     */
    public void setCanceled(boolean canceled) {
        progressMonitor.setCanceled(canceled);
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#setTaskName(String)
     */
    public void setTaskName(String taskName) {
        progressMonitor.setTaskName(taskName);
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#setSubTaskName(String)
     */
    public void setSubTaskName(String subTaskName) {
        progressMonitor.setSubTaskName(subTaskName);
    }

    /**
     * This implementation of a <code>ProgressMonitor</code>
     * method forwards to the wrapped progress monitor.
     * Clients may override this method to do additional
     * processing.
     *
     * @see ProgressMonitor#worked(int)
     */
    public void worked(int work) {
        progressMonitor.worked(work);
    }
}
