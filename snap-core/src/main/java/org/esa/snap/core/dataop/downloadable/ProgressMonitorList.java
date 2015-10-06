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

import java.util.ArrayList;
import java.util.List;

/**

 */
public class ProgressMonitorList {

    private static ProgressMonitorList _instance = null;
    private List<StatusProgressMonitor> list = new ArrayList<>();

    public enum Notification { ADD, REMOVE }
    private final List<Listener> listenerList = new ArrayList<>();

    public static ProgressMonitorList instance() {
        if (_instance == null) {
            _instance = new ProgressMonitorList();
        }
        return _instance;
    }

    private ProgressMonitorList() {

    }

    public void add(final StatusProgressMonitor pm) {
        list.add(pm);
        fireNotification(Notification.ADD, pm);
    }

    public void remove(final StatusProgressMonitor pm) {
        if(list.contains(pm)) {
            list.remove(pm);
            fireNotification(Notification.REMOVE, pm);
        }
    }

    public StatusProgressMonitor[] getList() {
        return list.toArray(new StatusProgressMonitor[list.size()]);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void addListener(final Listener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void fireNotification(final Notification msg, final StatusProgressMonitor pm) {
        for (Listener listener : listenerList) {
            listener.notifyMsg(msg, pm);
        }
    }

    public interface Listener {
        void notifyMsg(final Notification msg, final StatusProgressMonitor pm);
    }
}
