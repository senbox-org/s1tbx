package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.PlacemarkLayer;

import java.awt.Color;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PlacemarkLayerEditor extends AbstractBindingLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, BindingContext bindingContext) {
        ValueDescriptor vd0 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vd0.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_ENABLED);
        vd0.setDisplayName("Text enabled");
        addValueDescriptor(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vd1.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_FG_COLOR);
        vd1.setDisplayName("Text foreground colour");
        addValueDescriptor(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vd2.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_BG_COLOR);
        vd2.setDisplayName("Text background colour");
        addValueDescriptor(vd2);

    }

}
