package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;

import java.awt.*;

// todo - implement TrackLayerEditor (nf)

/**
* @author Norman Fomferra
*/
public class TrackLayerEditor extends VectorDataLayerEditor {
    @Override
    protected void addEditablePropertyDescriptors() {
        super.addEditablePropertyDescriptors();

        final PropertyDescriptor connectionLineWidth = new PropertyDescriptor("line-width", Double.class);
        connectionLineWidth.setDefaultValue(1.0);
        connectionLineWidth.setDefaultConverter();  // why this???
        addPropertyDescriptor(connectionLineWidth);

        final PropertyDescriptor connectionLineOpacity = new PropertyDescriptor("line-opacity", Double.class);
        connectionLineOpacity.setDefaultValue(0.7);
        connectionLineOpacity.setDefaultConverter();   // why this???
        addPropertyDescriptor(connectionLineOpacity);

        final PropertyDescriptor connectionLineColor = new PropertyDescriptor("line-color", Color.class);
        connectionLineOpacity.setDefaultValue(Color.ORANGE);
        connectionLineOpacity.setDefaultConverter();    // why this???
        addPropertyDescriptor(connectionLineColor);
    }

    @Override
    protected void updateProperties(SimpleFeatureFigure[] selectedFigures, BindingContext bindingContext) {
        super.updateProperties(selectedFigures, bindingContext);
        // todo - implement this (nf)
        //updateProperty(bindingContext, "line-width", ...);
        //updateProperty(bindingContext, "line-opacity", ...);
        //updateProperty(bindingContext, "line-color", ...);
    }

}
