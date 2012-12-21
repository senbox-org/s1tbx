/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.visat.VisatApp;

/**
 * status bar Progress monitor
 * This is used for showing progress on sub threads within an operator execution
 */
public final class StatusProgressMonitor {
    private final VisatApp visatApp = VisatApp.getApp();
    private final float max;
    private final String msg;
    private int lastPct = 0;
    private boolean allowStdOut = true;

    private int workInc = 0;

    public StatusProgressMonitor(final float max, final String msg) {
        this.max = max;
        this.msg = msg;
    }

    public synchronized void workedOne() {
        ++workInc;
        worked(workInc);
    }

    public synchronized void worked(final int i) {

        if(visatApp != null) {
            final int pct = (int)((i/max) * 100);
            if(pct >= lastPct + 1) {
                setText(msg+pct+'%');
                lastPct = pct;
            }
        } else if(allowStdOut) {
            final int pct = (int)((i/max) * 100);
            if(pct >= lastPct + 10) {
                if(lastPct==0) {
                    System.out.print(msg);
                }
                System.out.print(" "+pct+'%');
                lastPct = pct;
            }
        }
    }

    public void working() {
        if(visatApp != null) {
            setText(msg);
        }
    }

    public void done() {
        if(visatApp != null) {
            setText(" ");
        } else if(allowStdOut) {
            System.out.println(" 100%");
        }
    }

    public void setAllowStdOut(final boolean flag) {
        allowStdOut = flag;
    }

    private void setText(final String msg) {
        if(!ProgressMonitorList.instance().isEmpty()) {
            final ProgressMonitor[] pmList = ProgressMonitorList.instance().getList();
            for(ProgressMonitor pm : pmList) {
                pm.setTaskName(msg);
            }
        } else {
            visatApp.setStatusBarMessage(msg);
        }
    }

}