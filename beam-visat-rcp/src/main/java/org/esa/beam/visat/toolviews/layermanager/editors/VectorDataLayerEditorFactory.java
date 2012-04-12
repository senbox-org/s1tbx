package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.layer.AbstractLayerEditor;
import org.esa.beam.framework.ui.layer.LayerEditor;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.VectorDataLayer;

import javax.swing.*;
import java.awt.*;

/**
 * Experimental code: A factory that creates a specific {@link LayerEditor} for a given {@link VectorDataLayer}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class VectorDataLayerEditorFactory implements ExtensionFactory {
    @Override
    public LayerEditor getExtension(Object object, Class<?> extensionType) {
        if (object instanceof VectorDataLayer) {
            VectorDataLayer vectorDataLayer = (VectorDataLayer) object;
            String featureTypeName = vectorDataLayer.getVectorDataNode().getFeatureType().getTypeName();
            if (featureTypeName.equals("org.esa.beam.Placemark")) {
                return new AbstractLayerEditor() {
                    @Override
                    protected JComponent createControl() {
                        return new JLabel("I am an editor for features of type Placemark");
                    }
                };
            } else if (featureTypeName.equals("org.esa.beam.TrackPoint")) {
                return new VectorDataLayerEditor() {
                    @Override
                    protected void addEditablePropertyDescriptors() {
                        super.addEditablePropertyDescriptors();

                        final PropertyDescriptor connectionLineWidth = new PropertyDescriptor("connectionLineWidth", Double.class);
                        connectionLineWidth.setDefaultValue(1.0);
                        connectionLineWidth.setDefaultConverter();  // why this???
                        addPropertyDescriptor(connectionLineWidth);

                        final PropertyDescriptor connectionLineOpacity = new PropertyDescriptor("connectionLineOpacity", Double.class);
                        connectionLineOpacity.setDefaultValue(0.7);
                        connectionLineOpacity.setDefaultConverter();   // why this???
                        addPropertyDescriptor(connectionLineOpacity);

                        final PropertyDescriptor connectionLineColor = new PropertyDescriptor("connectionLineColor", Color.class);
                        connectionLineOpacity.setDefaultValue(Color.ORANGE);
                        connectionLineOpacity.setDefaultConverter();    // why this???
                        addPropertyDescriptor(connectionLineColor);
                    }

                    @Override
                    protected void updateProperties(SimpleFeatureFigure[] selectedFigures, BindingContext bindingContext) {
                        super.updateProperties(selectedFigures, bindingContext);

                        //updateProperty(bindingContext, "connectionLineWidth", ...);
                        //updateProperty(bindingContext, "connectionLineOpacity", ...);
                        //updateProperty(bindingContext, "connectionLineColor", ...);
                    }
                };
            } else {
                return new VectorDataLayerEditor();
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[]{LayerEditor.class};
    }
}
