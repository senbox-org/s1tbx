package org.esa.beam.smos.visat;

import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

public class SmosBox implements VisatPlugIn {


    private static SmosBox instance;

    private SnapshotSelectionService snapshotSelectionService;
    private GridPointSelectionService gridPointSelectionService;
    private SmosSceneViewSelectionService smosSceneViewSelectionService;

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

    public SmosSceneViewSelectionService getSmosViewSelectionService() {
        return smosSceneViewSelectionService;
    }

    public void start(VisatApp visatApp) {
        instance = this;
        smosSceneViewSelectionService = new SmosSceneViewSelectionService(visatApp);
        snapshotSelectionService = new SnapshotSelectionService(visatApp.getProductManager());
        gridPointSelectionService = new GridPointSelectionService();
    }

    public void stop(VisatApp visatApp) {
        smosSceneViewSelectionService.stop();
        smosSceneViewSelectionService = null;
        snapshotSelectionService.stop();
        snapshotSelectionService = null;
        gridPointSelectionService.stop();
        gridPointSelectionService = null;
        instance = null;
    }

    public void updateComponentTreeUI() {
    }

}
