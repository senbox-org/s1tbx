/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import com.bc.ceres.core.ProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**

 */
public class ProgressMonitorList {

    private static ProgressMonitorList _instance = null;
    private List<ProgressMonitor> list = new ArrayList<ProgressMonitor>(1);

    public static ProgressMonitorList instance() {
        if (_instance == null) {
            _instance = new ProgressMonitorList();
        }
        return _instance;
    }

    private ProgressMonitorList() {

    }

    public void add(final ProgressMonitor pm) {
        list.add(pm);
    }

    public void remove(final ProgressMonitor pm) {
        list.remove(pm);
    }

    public ProgressMonitor[] getList() {
        return list.toArray(new ProgressMonitor[list.size()]);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
