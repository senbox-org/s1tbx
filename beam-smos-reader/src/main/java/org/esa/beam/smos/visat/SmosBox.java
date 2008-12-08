package org.esa.beam.smos.visat;

import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

public class SmosBox implements VisatPlugIn {


    private static SmosBox instance;

    private SnapshotSelectionService snapshotSelectionService;
    private GridPointSelectionService gridPointSelectionService;
    private SceneViewSelectionService sceneViewSelectionService;

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

    public SceneViewSelectionService getSmosViewSelectionService() {
        return sceneViewSelectionService;
    }

    public void start(VisatApp visatApp) {
        instance = this;
        sceneViewSelectionService = new SceneViewSelectionService(visatApp);
        snapshotSelectionService = new SnapshotSelectionService(visatApp.getProductManager());
        gridPointSelectionService = new GridPointSelectionService();
    }

    public void stop(VisatApp visatApp) {
        sceneViewSelectionService.stop();
        sceneViewSelectionService = null;
        snapshotSelectionService.stop();
        snapshotSelectionService = null;
        gridPointSelectionService.stop();
        gridPointSelectionService = null;
        instance = null;
    }

    public void updateComponentTreeUI() {
    }

}
