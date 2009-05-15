package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.ui.AppContext;

import java.awt.Color;

/**
 * Basic editor for image layers.
 *
 * @author Ralf Quast
 * @version $ Revision: $ $ Date: $
 * @since BEAM 4.6
 */
public class ImageLayerEditor extends AbstractBindingLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, final BindingContext bindingContext) {
        ValueDescriptor vd0 = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class);
        vd0.setDefaultValue(ImageLayer.DEFAULT_BORDER_SHOWN);
        vd0.setDisplayName("Show image border");
        vd0.setDefaultConverter();
        addValueDescriptor(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class);
        vd1.setDefaultValue(ImageLayer.DEFAULT_BORDER_COLOR);
        vd1.setDisplayName("Image border colour");
        vd1.setDefaultConverter();
        addValueDescriptor(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class);
        vd2.setDefaultValue(ImageLayer.DEFAULT_BORDER_WIDTH);
        vd2.setDisplayName("Image border size");
        vd2.setDefaultConverter();
        addValueDescriptor(vd2);
    }
}