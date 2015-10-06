/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.dataop.downloadable;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * status bar Progress monitor
 * This is used for showing progress on sub threads within an operator execution
 */
public final class StatusProgressMonitor implements ProgressMonitor {

    private int lastPct = 0;

    private String name;
    private double currentWork;
    private double totalWork;
    private boolean cancelRequested;
    private String text;

    public enum TYPE { DATA_TRANSFER, SUBTASK }
    private final TYPE taskType;

    public enum Notification { UPDATE, DONE }
    private final List<Listener> listenerList = new ArrayList<>();

    public StatusProgressMonitor() {
        this(TYPE.SUBTASK);
    }

    public StatusProgressMonitor(final TYPE taskType) {
        this.taskType = taskType;
    }

    @Override
    protected void finalize() throws Throwable {
        ProgressMonitorList.instance().remove(this);

        super.finalize();
    }

    /**
     * Notifies that the main task is beginning.  This must only be called once
     * on a given progress monitor instance.
     *
     * @param name      the name (or description) of the main task
     * @param totalWork the total number of work units into which
     *                  the main task is been subdivided. If the value is <code>UNKNOWN</code>
     *                  the implementation is free to indicate progress in a way which
     *                  doesn't require the total number of work units in advance.
     */
    @Override
    public void beginTask(final String name, final int totalWork) {
        Assert.notNull(name, "name");
        this.name = name;
        this.currentWork = 0.0;
        this.totalWork = totalWork;
        this.cancelRequested = false;
        this.text = "";
        ProgressMonitorList.instance().add(this);
    }

    /**
     * Internal method to handle scaling correctly. This method
     * must not be called by a client. Clients should
     * always use the method <code>worked(int)</code>.
     *
     * @param work the amount of work done
     */
    @Override
    public void internalWorked(double work) {
        currentWork += work;
        final int pct = (int) ((currentWork / totalWork) * 100);
        if (pct >= lastPct + 1) {
            setText(name + pct + '%');
            lastPct = pct;
        }
    }

    /**
     * Returns whether cancelation of current operation has been requested.
     * Long-running operations should poll to see if cancelation
     * has been requested.
     *
     * @return <code>true</code> if cancellation has been requested,
     *         and <code>false</code> otherwise
     *
     * @see #setCanceled(boolean)
     */
    @Override
    public boolean isCanceled() {
        return cancelRequested;
    }

    /**
     * Sets the cancel state to the given value.
     *
     * @param canceled <code>true</code> indicates that cancelation has
     *                 been requested (but not necessarily acknowledged);
     *                 <code>false</code> clears this flag
     *
     * @see #isCanceled()
     */
    @Override
    public void setCanceled(boolean canceled) {
        this.cancelRequested = canceled;
    }

    /**
     * Sets the task name to the given value. This method is used to
     * restore the task label after a nested operation was executed.
     * Normally there is no need for clients to call this method.
     *
     * @param taskName the name (or description) of the main task
     *
     * @see #beginTask(String, int)
     */
    @Override
    public void setTaskName(String taskName) {

    }

    @Override
    public void setSubTaskName(String subTaskName) {

    }

    @Override
    public void worked(int work) {
        internalWorked(work);
    }

    /**
     * Notifies that the work is done; that is, either the main task is completed
     * or the user canceled it. This method may be called more than once
     * (implementations should be prepared to handle this case).
     */
    @Override
    public void done() {
        setText("");
        fireNotification(Notification.DONE);
        ProgressMonitorList.instance().remove(this);
    }

    private void setText(final String text) {
        this.text = text;
        fireNotification(Notification.UPDATE);
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public TYPE getTaskType() {
        return taskType;
    }

    public int getMax() {
        return (int)totalWork;
    }

    public int getPercentComplete() {
        return lastPct;
    }

    public void addListener(final Listener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void fireNotification(final Notification msg) {
        for (Listener listener : listenerList) {
            listener.notifyMsg(msg);
        }
    }

    public interface Listener {
        void notifyMsg(final Notification msg);
    }

}