package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import org.esa.beam.glayer.PlacemarkLayer;

import java.awt.Color;
import java.util.List;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PlacemarkLayerEditor extends ValueDescriptorLayerEditor {

    @Override
    protected void collectValueDescriptors(List<ValueDescriptor> descriptorList) {
        ValueDescriptor vd0 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vd0.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_ENABLED);
        vd0.setDisplayName("Text enabled");
        descriptorList.add(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vd1.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_FG_COLOR);
        vd1.setDisplayName("Text foreground color");
        descriptorList.add(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vd2.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_BG_COLOR);
        vd2.setDisplayName("Text background color");
        descriptorList.add(vd2);

    }

}
