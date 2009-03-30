package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.glayer.Layer;

import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import java.awt.Color;

import javax.swing.JComponent;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PlacemarkLayerEditor implements LayerEditor {

    private final ValueDescriptorLayerEditor delegate;
    
    public PlacemarkLayerEditor() {
        delegate = new ValueDescriptorLayerEditor(createVDs());
    }
    
    @Override
    public JComponent createControl() {
        return delegate.createControl();
    }

    @Override
    public void updateControl(Layer selectedLayer) {
        delegate.updateControl(selectedLayer);
    }
    
    private static ValueDescriptor[] createVDs() {
        ValueDescriptor[] valueDescriptors = new ValueDescriptor[3];
        
        valueDescriptors[0] = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        valueDescriptors[0].setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_ENABLED);
        valueDescriptors[0].setDisplayName("Text enabled");
        
        valueDescriptors[1] = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        valueDescriptors[1].setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_FG_COLOR);
        valueDescriptors[1].setDisplayName("Text foreground color");
        
        valueDescriptors[2] = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        valueDescriptors[2].setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_BG_COLOR);
        valueDescriptors[2].setDisplayName("Text background color");
        
        return valueDescriptors;
    }
}
