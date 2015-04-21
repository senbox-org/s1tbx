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
 * A progress monitor that uses a given amount of work ticks
 * from a parent monitor. It can be used as follows:
 * <pre>
 *     try {
 *         pm.beginTask("Main Task", 100);
 *         doSomeWork(pm, 30);
 *         SubProgressMonitor subMonitor= SubProgressMonitor.create(pm, 40);
 *         try {
 *             subMonitor.beginTask("", 300);
 *             doSomeWork(subMonitor, 300);
 *         } finally {
 *             subMonitor.done();
 *         }
 *         doSomeWork(pm, 30);
 *     } finally {
 *         pm.done();
 *     }
 * </pre>
 * <p>
 * This class may be instantiated or subclassed by clients.
 * <p>This class has been more or less directly taken over from the <a href="http://www.eclipse.org/">Eclipse</a> Core API.
 */
public class SubProgressMonitor extends ProgressMonitorWrapper {

    /**
     * Style constant indicating that calls to <code>subTask</code>
     * should not have any effect.
     *
     * @see #SubProgressMonitor(ProgressMonitor,int,int)
     */
    public static final int SUPPRESS_SUBTASK_LABEL = 1 << 1;
    /**
     * Style constant indicating that the main task label
     * should be prepended to the subtask label.
     *
     * @see #SubProgressMonitor(ProgressMonitor,int,int)
     */
    public static final int PREPEND_MAIN_LABEL_TO_SUBTASK = 1 << 2;

    private int parentTicks = 0;
    private double sentToParent = 0.0;
    private double scale = 0.0;
    private int nestedBeginTasks = 0;
    private boolean usedUp = false;
    private boolean hasSubTask = false;
    private int style;
    private String taskName;
    private int totalWork;
    private long t0;
    private final static boolean traceTimeStat = Boolean.getBoolean("com.bc.ceres.core.SubProgressMonitor.traceTimeStat");

    /**
     * Creates a progress monitor based on the passed in parent monitor. If the parent monitor is {@link ProgressMonitor#NULL},
     * <code>monitor</code> is returned, otherwise a new <code>SubProgressMonitor</code> is created.
     * @param monitor the parent progress monitor
     * @param ticks   the number of work ticks allocated from the
     *                parent monitor
     * @return a progress monitor
     */
    public static ProgressMonitor create(ProgressMonitor monitor, int ticks) {
        if (monitor == ProgressMonitor.NULL) {
            return monitor;
        }
        return new SubProgressMonitor(monitor, ticks);
    }

    /**
     * Creates a new sub-progress monitor for the given monitor. The sub
     * progress monitor uses the given number of work ticks from its
     * parent monitor.
     *
     * @param monitor the parent progress monitor
     * @param ticks   the number of work ticks allocated from the
     *                parent monitor
     */
    public SubProgressMonitor(ProgressMonitor monitor, int ticks) {
        this(monitor, ticks, 0);
    }

    /**
     * Creates a new sub-progress monitor for the given monitor. The sub
     * progress monitor uses the given number of work ticks from its
     * parent monitor.
     *
     * @param monitor the parent progress monitor
     * @param ticks   the number of work ticks allocated from the
     *                parent monitor
     * @param style   one of
     *                <ul>
     *                <li> <code>SUPPRESS_SUBTASK_LABEL</code> </li>
     *                <li> <code>PREPEND_MAIN_LABEL_TO_SUBTASK</code> </li>
     *                </ul>
     * @see #SUPPRESS_SUBTASK_LABEL
     * @see #PREPEND_MAIN_LABEL_TO_SUBTASK
     */
    public SubProgressMonitor(ProgressMonitor monitor, int ticks, int style) {
        super(monitor);
        this.parentTicks = ticks;
        this.style = style;
    }

    /* (Intentionally not javadoc'd)
      * Implements the method <code>ProgressMonitor.beginTask</code>.
      *
      * Starts a new main task. Since this progress monitor is a sub
      * progress monitor, the given name will NOT be used to update
      * the progress bar's main task label. That means the given
      * string will be ignored. If style <code>PREPEND_MAIN_LABEL_TO_SUBTASK
      * <code> is specified, then the given string will be prepended to
      * every string passed to <code>subTask(String)</code>.
      */
    @Override
    public void beginTask(String taskName, int totalWork) {
        nestedBeginTasks++;
        // Ignore nested begin task calls.
        if (nestedBeginTasks > 1) {
            return;
        }
        t0 = System.currentTimeMillis();
        this.taskName = taskName;
        this.totalWork = totalWork;
        // be safe:  if the argument would cause math errors (zero or
        // negative), just use 0 as the scale.  This disables progress for
        // this submonitor.
        scale = totalWork <= 0 ? 0 : (double) parentTicks / (double) totalWork;
    }

    /* (Intentionally not javadoc'd)
      * Implements the method <code>ProgressMonitor.done</code>.
      */
    @Override
    public void done() {
        // Ignore if more done calls than beginTask calls or if we are still
        // in some nested beginTasks
        if (nestedBeginTasks == 0 || --nestedBeginTasks > 0) {
            return;
        }
        // Send any remaining ticks and clear out the subtask text
        double remaining = parentTicks - sentToParent;
        if (remaining > 0) {
            super.internalWorked(remaining);
        }
        //clear the sub task if there was one
        if (hasSubTask) {
            setSubTaskName(""); //$NON-NLS-1$
        }
        sentToParent = 0;

        if (traceTimeStat) {
            long dt = System.currentTimeMillis() - t0;
            System.out.println("Task '" + taskName + "':");
            System.out.println("  ParentTicks:      " + parentTicks);
            System.out.println("  Total work:       " + totalWork);
            System.out.println("  Total time:       " + dt + " ms");
            System.out.println("  Time / work unit: " + ((double)dt / (double)totalWork) + " ms");
        }
    }

    /* (Intentionally not javadoc'd)
      * Implements the internal method <code>ProgressMonitor.internalWorked</code>.
      */
    @Override
    public void internalWorked(double work) {
        if (usedUp || nestedBeginTasks != 1) {
            return;
        }

        double realWork = scale * work;
        super.internalWorked(realWork);
        sentToParent += realWork;
        if (sentToParent >= parentTicks) {
            usedUp = true;
        }
    }

    /* (Intentionally not javadoc'd)
      * Implements the method <code>ProgressMonitor.subTask</code>.
      */
    @Override
    public void setSubTaskName(String subTaskName) {
        if ((style & SUPPRESS_SUBTASK_LABEL) != 0) {
            return;
        }
        hasSubTask = true;
        String label = subTaskName;
        if ((style & PREPEND_MAIN_LABEL_TO_SUBTASK) != 0 && taskName != null && taskName.length() > 0) {
            label = taskName + ' ' + label;
        }
        super.setSubTaskName(label);
    }

    /* (Intentionally not javadoc'd)
      * Implements the method <code>ProgressMonitor.worked</code>.
      */
    @Override
    public void worked(int work) {
        internalWorked(work);
	}
}
