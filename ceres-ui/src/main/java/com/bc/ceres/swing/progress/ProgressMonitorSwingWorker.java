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

package com.bc.ceres.swing.progress;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.concurrent.CountDownLatch;


/**
 * A swing worker which may pop-up a progress monitor dialog.
 */
public abstract class ProgressMonitorSwingWorker<T, V> extends SwingWorker<T, V> {

    private final Component parentComponent;
    private final String title;
    private boolean blocking;
    private Window blockingWindow;
    private final CountDownLatch unBlock = new CountDownLatch(1);


    protected ProgressMonitorSwingWorker(Component parentComponent, String title) {
        this.parentComponent = parentComponent;
        this.title = title;
    }

    /**
     * Overridden in order to call the {@link #doInBackground(com.bc.ceres.core.ProgressMonitor) doInBackground}
     * method with a {@link com.bc.ceres.core.ProgressMonitor ProgressMonitor}.
     *
     * @return the computed result
     *
     * @throws Exception
     */
    @Override
    protected final T doInBackground() throws Exception {
        DialogProgressMonitor pm = new DialogProgressMonitor(parentComponent, title,
                                                             blocking ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
        T value;
        try {
            value = doInBackground(pm);
        } finally {
            pm.done();
            if (blocking) {
                unBlock.await();
                unblock();
            }
        }
        return value;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     * <p/>
     * <p>Note that this method is executed only once in a background thread.</p>
     *
     * @param pm the progress monitor
     *
     * @return the computed result
     *
     * @throws Exception if unable to compute a result
     */
    protected abstract T doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception;

    /**
     * Similar to the {@link #execute()} method, but blocks the current thread until
     * this worker computed its result. However, this method will not
     * block the <i>Event Dispatch Thread</i>.
     */
    public final void executeWithBlocking() {
        this.blocking = true;
        execute();
        block();
        this.blocking = false;
    }


    private void block() {
        if (blockingWindow == null) {
            blockingWindow = createBlockingWindow();
            unBlock.countDown();
            blockingWindow.setVisible(true);
        }
    }

    private void unblock() {
        if (blockingWindow != null) {
            blockingWindow.dispose();
            blockingWindow = null;
        }
    }

    private static Window createBlockingWindow() {
        final Dialog window = new Dialog((Frame) null);
        window.setUndecorated(true);
        window.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        window.setBounds(0, 0, 0, 0);
        window.setFocusableWindowState(false);
        return window;
    }
}
