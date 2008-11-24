package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.Window;


class BandLayerProvider implements LayerProvider {

    private static final String BAND_OVERLAYS = "Band overlays";
    Product product;
    private LayerManagerToolView layerManagerToolView;

    public BandLayerProvider(LayerManagerToolView layerManagerToolView, Product product) {
        this.layerManagerToolView = layerManagerToolView;
        this.product = product;

    }

    public void addLayers(Window window, LayerTreeModel treeModel, Layer selectedLayer) {
        Layer rootLayer = treeModel.getRootLayer();

        String[] strings = product.getBandNames();
        JList list = new JList(strings);
        ModalDialog dialog = new ModalDialog(window, "Select Overlay Band", new JScrollPane(list), ModalDialog.ID_OK + ModalDialog.ID_CANCEL, null);
        int result = dialog.show();
        int index = list.getSelectedIndex();
        if (result == ModalDialog.ID_OK && index != -1) {

            Layer bandOverlaysLayer = treeModel.getLayer(BAND_OVERLAYS);
            if (bandOverlaysLayer == null) {
                bandOverlaysLayer = new Layer();
                bandOverlaysLayer.setName(BAND_OVERLAYS);
                bandOverlaysLayer.setVisible(true);
                rootLayer.getChildren().add(bandOverlaysLayer);
            }

            Band band = product.getBand(strings[index]);
            BandImageMultiLevelSource bandImageMultiLevelSource = BandImageMultiLevelSource.create(band, ProgressMonitor.NULL);
            ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

            imageLayer.setName(band.getName());
            imageLayer.setVisible(true);

            bandOverlaysLayer.getChildren().add(imageLayer);
        }
    }

    public void removeLayers(Window window, LayerTreeModel treeModel, Layer selectedLayer) {
        if (selectedLayer != null) {
            Layer bandOverlaysLayer = treeModel.getLayer(BAND_OVERLAYS);
            if (bandOverlaysLayer.getChildren().contains(selectedLayer)) {
                // todo warn
                bandOverlaysLayer.getChildren().remove(selectedLayer);
            } else {
                // todo warn
            }
        }
    }

}
