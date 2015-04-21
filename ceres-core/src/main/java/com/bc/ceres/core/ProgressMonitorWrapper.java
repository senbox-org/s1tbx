/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core;

/**
 * An abstract wrapper around a progress monitor which,
 * unless overridden, forwards <code>IProgressMonitor</code>
 * and <code>IProgressMonitorWithBlocking</code> methods to the wrapped progress monitor.
 * <p>
 * Clients may subclass.
 *
 * <p>This class has been more or less directly taken over from the <a href="http://www.eclipse.org/">Eclipse</a> Core API.
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
