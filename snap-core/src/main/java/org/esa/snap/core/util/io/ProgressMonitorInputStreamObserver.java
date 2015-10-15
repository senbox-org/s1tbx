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
package org.esa.snap.core.util.io;

import javax.swing.ProgressMonitor;

public class ProgressMonitorInputStreamObserver implements InputStreamObserver {
    private ProgressMonitor progressMonitor;

    public ProgressMonitorInputStreamObserver(ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public void onReadStarted(long numBytesTotal) {
        progressMonitor.setMinimum(0);
        progressMonitor.setMaximum((int) numBytesTotal);
    }

    public void onReadProgress(long numBytesRead) {
        progressMonitor.setProgress((int) numBytesRead);
    }

    public void onReadEnded() {
        progressMonitor.close();
    }

    public boolean isReadingCanceled() {
        return progressMonitor.isCanceled();
    }
}
