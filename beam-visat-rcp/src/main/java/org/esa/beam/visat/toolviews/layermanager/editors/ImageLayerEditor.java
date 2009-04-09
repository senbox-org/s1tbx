package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import javax.swing.JComponent;
import java.awt.Color;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ImageLayerEditor implements LayerEditor {

    private final ValueDescriptorLayerEditor delegate;

    public ImageLayerEditor() {
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

        valueDescriptors[0] = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class);
        valueDescriptors[0].setDefaultValue(ImageLayer.DEFAULT_BORDER_SHOWN);
        valueDescriptors[0].setDisplayName("Show image border");
        valueDescriptors[0].setDefaultConverter();

        valueDescriptors[1] = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class);
        valueDescriptors[1].setDefaultValue(ImageLayer.DEFAULT_BORDER_COLOR);
        valueDescriptors[1].setDisplayName("Image border colour");
        valueDescriptors[1].setDefaultConverter();

        valueDescriptors[2] = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class);
        valueDescriptors[2].setDefaultValue(ImageLayer.DEFAULT_BORDER_WIDTH);
        valueDescriptors[2].setDisplayName("Image border size");
        valueDescriptors[2].setDefaultConverter();

        return valueDescriptors;
    }
}