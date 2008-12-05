package org.esa.beam.smos.visat;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.visat.VisatApp;

public class SmosBox implements Activator {

    private static SmosBox instance;

    private SnapshotSelectionService snapshotSelectionService;

    public SmosBox() {
    }

    public static SmosBox getInstance() {
        return instance;
    }

    public SnapshotSelectionService getSnapshotSelectionService() {
        return snapshotSelectionService;
    }

    public void start(ModuleContext moduleContext) throws CoreException {
        instance = this;
        snapshotSelectionService = new SnapshotSelectionService(VisatApp.getApp().getProductManager());
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        snapshotSelectionService.dispose();
        snapshotSelectionService = null;
        instance = null;
    }
}
