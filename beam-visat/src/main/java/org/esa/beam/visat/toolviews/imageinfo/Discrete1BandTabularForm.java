package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JTable;
import java.awt.Component;

class Discrete1BandTabularForm implements PaletteEditorForm {
    private ImageInfo currentImageInfo;
    private JTable contentPanel;

    public Discrete1BandTabularForm() {
        contentPanel = new JTable(new Object[][]{}, new Object[] {"Value", "Colour", "Label", "Freq."});
    }

    public void performApply(ProductSceneView productSceneView) {

    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{new JButton(":D")};
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public ImageInfo getCurrentImageInfo() {
        return currentImageInfo;
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {

        currentImageInfo = imageInfo;
    }

    public String getTitle(ProductSceneView productSceneView) {
        return ":P";
    }

    public void handleFormShown(ProductSceneView productSceneView) {
    }

    public void handleFormHidden() {
    }

    public void performReset(ProductSceneView productSceneView) {
    }

    public void updateState(ProductSceneView productSceneView) {
    }
}