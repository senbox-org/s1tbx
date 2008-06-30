package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.ui.product.ProductSceneView;

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

    public AbstractButton[] getButtons() {
        return new AbstractButton[0];
    }

    public Component getContentPanel() {
        return new JLabel("No image view selected.");
    }
}