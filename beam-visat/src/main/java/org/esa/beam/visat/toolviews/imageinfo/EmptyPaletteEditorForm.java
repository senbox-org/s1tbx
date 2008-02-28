package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;

class EmptyPaletteEditorForm implements PaletteEditorForm {
    public static final PaletteEditorForm INSTANCE = new EmptyPaletteEditorForm();

    private EmptyPaletteEditorForm() {
    }

    public void performApply(ProductSceneView productSceneView) {
    }

    public void performReset(ProductSceneView productSceneView) {
    }

    public ImageInfo getCurrentImageInfo() {
        return null;
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
    }

    public void handleFormShown(ProductSceneView productSceneView) {
    }

    public void handleFormHidden() {
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[0];
    }

    public void updateState(ProductSceneView productSceneView) {
    }

    public Component getContentPanel() {
        return new JLabel("No image view selected.");
    }

    public String getTitle(ProductSceneView productSceneView) {
        return "Empty";
    }

}