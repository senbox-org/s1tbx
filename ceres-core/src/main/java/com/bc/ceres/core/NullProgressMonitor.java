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
 * A default progress monitor implementation suitable for subclassing.
 * <p>
 * This implementation supports cancellation. The default implementations of
 * the other methods do nothing.
 * <p>
 * This class has been more or less directly taken over from the
 * <a href="http://www.eclipse.org/">Eclipse</a> Core API.
 */
public class NullProgressMonitor implements ProgressMonitor {

    /**
     * Indicates whether cancel has been requested.
     */
    private boolean canceled = false;

    /**
     * Constructs a new progress monitor.
     */
    public NullProgressMonitor() {
        super();
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method to do interesting
     * processing when a task begins.
     */
    public void beginTask(String taskName, int totalWork) {
        // do nothing
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method to do interesting
     * processing when a task is done.
     */
    public void done() {
        // do nothing
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method.
     */
    public void internalWorked(double work) {
        // do nothing
    }

    /**
     * This implementation returns the value of the internal
     * state variable set by <code>setCanceled</code>.
     * Subclasses which override this method should
     * override <code>setCanceled</code> as well.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * This implementation sets the value of an internal state variable.
     * Subclasses which override this method should override
     * <code>isCanceled</code> as well.
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method to do something
     * with the name of the task.
     */
    public void setTaskName(String taskName) {
        // do nothing
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method to do interesting
     * processing when a subtask begins.
     */
    public void setSubTaskName(String subTaskName) {
        // do nothing
    }

    /**
     * This implementation does nothing.
     * Subclasses may override this method to do interesting
     * processing when some work has been completed.
     */
    public void worked(int work) {
        // do nothing
    }
}
