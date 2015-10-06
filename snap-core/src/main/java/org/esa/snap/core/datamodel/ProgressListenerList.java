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

import java.util.ArrayList;
import java.util.List;


/**
 * A utility class for clients interested in the progress made while reading, writing or somehow processing data
 * products.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProgressListenerList implements ProgressListener {

    private List _listeners;

    public ProgressListenerList() {
    }

    /**
     * Don't call this method directly. Instead use {@link  #fireProcessStarted}.
     *
     * @param processDescription
     * @param minProgressValue
     * @param maxProgressValue
     *
     * @return <code>true</code> if the process is started, otherwise <code>false</code>.
     */
    public boolean processStarted(String processDescription, int minProgressValue, int maxProgressValue) {
        return fireProcessStarted(processDescription,
                                  minProgressValue,
                                  maxProgressValue);
    }

    /**
     * Don't call this method directly. Instead use {@link  #fireProcessInProgress}.
     * @param currentProgressValue
     * @return <code>true</code> if the process should be continued, <code>false</code> otherwise
     */
    public boolean processInProgress(int currentProgressValue) {
        return fireProcessInProgress(currentProgressValue);
    }

    /**
     * Don't call this method directly. Instead use {@link  #fireProcessEnded}.
     * @param success
     */
    public void processEnded(boolean success) {
        fireProcessEnded(success);
    }

    public void addProgressListener(ProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (_listeners == null) {
            _listeners = new ArrayList();
        }
        if (!_listeners.contains(listener)) {
            _listeners.add(listener);
        }
    }

    public void removeProgressListener(ProgressListener listener) {
        if (listener == null || _listeners == null) {
            return;
        }
        _listeners.remove(listener);
    }

    public void removeAllProgressListeners() {
        _listeners.clear();
    }

    public boolean fireProcessStarted(String processName, int minProgressValue, int maxProgressValue) {
        if (_listeners != null) {
            for (int i = 0; i < _listeners.size(); i++) {
                if (!((ProgressListener) _listeners.get(i)).processStarted(processName,
                                                                           minProgressValue,
                                                                           maxProgressValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean fireProcessInProgress(int currentProgressValue) {
        if (_listeners != null) {
            for (int i = 0; i < _listeners.size(); i++) {
                if (!((ProgressListener) _listeners.get(i)).processInProgress(currentProgressValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void fireProcessEnded(boolean success) {
        if (_listeners != null) {
            for (int i = 0; i < _listeners.size(); i++) {
                ((ProgressListener) _listeners.get(i)).processEnded(success);
            }
        }
    }

    public boolean isEmpty() {
        return _listeners.size() > 0;
    }
}
