package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;

class EmptyForm implements SpecificForm {
    public static final SpecificForm INSTANCE = new EmptyForm();

    private EmptyForm() {
    }

    public void apply() {
    }

    public void reset() {
    }

    public ImageInfo getCurrentImageInfo() {
        return null;
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
    }

    public void initProductSceneView(ProductSceneView productSceneView) {
    }

    public void releaseProductSceneView() {
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[0];
    }

    public void updateState() {
    }

    public Component getContentPanel() {
        return new JLabel("No image view selected.");
    }

    public String getTitle() {
        return "Empty";
    }

}