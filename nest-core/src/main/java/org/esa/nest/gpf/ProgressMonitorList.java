package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**

 */
public class ProgressMonitorList {

    private static ProgressMonitorList _instance = null;
    private List<ProgressMonitor> list = new ArrayList<ProgressMonitor>(1);

    public static ProgressMonitorList instance() {
        if(_instance == null) {
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
