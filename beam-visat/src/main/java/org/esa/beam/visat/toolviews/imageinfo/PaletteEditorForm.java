package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import java.awt.Component;


interface PaletteEditorForm {
    void handleFormShown(ProductSceneView productSceneView);

    void handleFormHidden();

    void performApply(ProductSceneView productSceneView);

    void performReset(ProductSceneView productSceneView);

    void updateState(ProductSceneView productSceneView);

    String getTitle(ProductSceneView productSceneView);

    ImageInfo getCurrentImageInfo();

    Component getContentPanel();

    AbstractButton[] getButtons();

}
