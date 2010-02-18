package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;

class EmptyImageInfoForm implements ColorManipulationChildForm {
    public static final ColorManipulationChildForm INSTANCE = new EmptyImageInfoForm();

    private EmptyImageInfoForm() {
    }

    public void handleFormShown(ProductSceneView productSceneView) {
    }

    public void handleFormHidden(ProductSceneView productSceneView) {
    }

    public void updateFormModel(ProductSceneView productSceneView) {
    }

    public void resetFormModel(ProductSceneView productSceneView) {
    }

    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
    }

    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    public Component getContentPanel() {
        return new JLabel("No image view selected.");
    }

    public RasterDataNode[] getRasters() {
        return new RasterDataNode[0];
    }

    public MoreOptionsForm getMoreOptionsForm() {
        return null;
    }
}
