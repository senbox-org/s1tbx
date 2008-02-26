package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Component;

class DiscreteForm implements SpecificForm {
    private ImageInfo currentImageInfo;
    private JLabel contentPanel;

    public DiscreteForm() {
        contentPanel = new JLabel("I am concrete dicrete!");
    }

    public void apply() {

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

    public String getTitle() {
        return ":P";
    }

    public void initProductSceneView(ProductSceneView productSceneView) {

    }

    public void releaseProductSceneView() {

    }

    public void reset() {

    }

    public void updateState() {

    }
}