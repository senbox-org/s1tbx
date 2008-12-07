package org.esa.beam.smos.visat;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.visat.VisatApp;

public class SmosBox implements Activator {

    private static SmosBox instance;

    private SnapshotSelectionService snapshotSelectionService;
    private GridPointSelectionService gridPointSelectionService;

    public SmosBox() {
    }

    public static SmosBox getInstance() {
        return instance;
    }

    public SnapshotSelectionService getSnapshotSelectionService() {
        return snapshotSelectionService;
    }

    public GridPointSelectionService getGridPointSelectionService() {
        return gridPointSelectionService;
    }

    public void start(ModuleContext moduleContext) throws CoreException {
        instance = this;
        snapshotSelectionService = new SnapshotSelectionService(VisatApp.getApp().getProductManager());
        gridPointSelectionService = new GridPointSelectionService();
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        snapshotSelectionService.dispose();
        snapshotSelectionService = null;
        gridPointSelectionService.dispose();
        gridPointSelectionService = null;
        instance = null;
    }
}
