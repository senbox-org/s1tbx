package org.esa.beam.visat.toolviews.cspal;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import java.awt.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 26.02.2008
 * Time: 13:47:52
 * To change this template use File | Settings | File Templates.
 */
public interface SpecificForm {
    void reset();

    ImageInfo getCurrentImageInfo();

    void initProductSceneView(ProductSceneView productSceneView);

    AbstractButton[] getButtons();

    void updateState();

    Component getContentPanel();

    void apply();

    String getTitle();

    void releaseProductSceneView();
}
