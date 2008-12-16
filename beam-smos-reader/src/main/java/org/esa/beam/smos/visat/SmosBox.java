package org.esa.beam.smos.visat;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

import java.io.File;
import java.io.IOException;

public class SmosBox implements VisatPlugIn {

    private static final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.pview.worldImageDir";
    static final String WORLD_MAP_LAYER_ID = "org.esa.beam.smos.layers.worldMap";

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

    @Override
    public void start(VisatApp visatApp) {
        instance = this;
        sceneViewSelectionService = new SceneViewSelectionService(visatApp);
        snapshotSelectionService = new SnapshotSelectionService(visatApp.getProductManager());
        gridPointSelectionService = new GridPointSelectionService();

        sceneViewSelectionService.addSceneViewSelectionListener(new SceneViewSelectionService.SelectionListener() {
            @Override
            public void handleSceneViewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
                if (newView != null) {
                    Layer rootLayer = newView.getRootLayer();
                    if (rootLayer.getChildIndex(WORLD_MAP_LAYER_ID) == -1) {
                        Layer worldLayer = createWorldMapLayer();
                        if (worldLayer != null) {
                            rootLayer.getChildren().add(worldLayer);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void stop(VisatApp visatApp) {
        sceneViewSelectionService.stop();
        sceneViewSelectionService = null;
        snapshotSelectionService.stop();
        snapshotSelectionService = null;
        gridPointSelectionService.stop();
        gridPointSelectionService = null;
        instance = null;
    }

    @Override
    public void updateComponentTreeUI() {
    }

    private Layer createWorldMapLayer() {
        String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
        if (dirPath == null || dirPath.isEmpty()) {
            return null;
        }
        MultiLevelSource multiLevelSource;
        try {
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
        } catch (IOException e) {
            return null;
        }
        final ImageLayer worldMapLayer = new ImageLayer(multiLevelSource);
        worldMapLayer.setName("World Map (NASA Blue Marble)");
        worldMapLayer.setVisible(true);
        worldMapLayer.setId(WORLD_MAP_LAYER_ID);
        worldMapLayer.getStyle().setOpacity(1.0);
        return worldMapLayer;
    }

}
