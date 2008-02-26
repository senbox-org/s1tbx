package org.esa.beam.visat.toolviews.cspal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;

public class EmptyForm implements SpecificForm {

    public EmptyForm() {
    }

    public void apply() {
    }

    public void reset() {
    }

    public ImageInfo getCurrentImageInfo() {
        return null;
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