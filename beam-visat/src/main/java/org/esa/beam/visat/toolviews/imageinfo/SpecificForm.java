package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import java.awt.Component;


interface SpecificForm {
    void reset();

    ImageInfo getCurrentImageInfo();

    void setCurrentImageInfo(ImageInfo imageInfo);

    void initProductSceneView(ProductSceneView productSceneView);

    AbstractButton[] getButtons();

    void updateState();

    Component getContentPanel();

    void apply();

    String getTitle();

    void releaseProductSceneView();

}
