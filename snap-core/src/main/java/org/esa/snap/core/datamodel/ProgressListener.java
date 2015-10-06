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
package org.esa.snap.core.datamodel;


/**
 * The <code>ProgressListener</code> interface represents a listener for the observation of possibly time consuming
 * processes.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface ProgressListener {

    /**
     * Called when a process started. If the process can be started, the listener should return <code>true</code>.
     *
     * @param processDescription a short description of the process beeing started
     * @param minProgressValue
     * @param maxProgressValue
     *
     * @return <code>true</code> if the process was successfully started (and should be continued), <code>false</code>
     *         otherwise
     */
    boolean processStarted(String processDescription, int minProgressValue, int maxProgressValue);

    /**
     * Called while a process in in progress. A listener should return <code>true</code> if the process can be
     * continued, <code>false</code> if it should be terminated.
     *
     * @param currentProgressValue the current progress value
     *
     * @return <code>true</code> if the process should be continued, <code>false</code> otherwise
     */
    boolean processInProgress(int currentProgressValue);

    /**
     * Called when a process ended.
     *
     * @param success if <code>true</code> the process was successfully ended, otherwise it was terminated by user
     *                demand or due to an exception.
     */
    void processEnded(boolean success);
}
