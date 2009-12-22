package org.esa.beam.framework.ui.product;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.LayerEditor;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;

public class VectorDataLayerEditor implements LayerEditor {

    @Override
    public JComponent createControl(AppContext appContext, Layer layer) {
        if (!(layer instanceof VectorDataLayer)) {
            return null;
        }

        final VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
        /*
        final FigureCollection figureCollection = vectorDataLayer.getFigureCollection();

        PropertySet  propertySet = PropertyContainer.c
        BindingContext ctx = new BindingContext(propertySet);

        final PropertySet propertySet = ctx.getPropertySet();
        propertySet.addProperty(Property.c);

        PropertyDescriptor vd0 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, Boolean.class);
        vd0.setDefaultValue(ImageLayer.DEFAULT_BORDER_SHOWN);
        vd0.setDisplayName("Show image border");
        vd0.setDefaultConverter();
        addPropertyDescriptor(vd0);

        PropertyDescriptor vd1 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_COLOR, Color.class);
        vd1.setDefaultValue(ImageLayer.DEFAULT_BORDER_COLOR);
        vd1.setDisplayName("Image border colour");
        vd1.setDefaultConverter();
        addPropertyDescriptor(vd1);

        PropertyDescriptor vd2 = new PropertyDescriptor(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, Double.class);
        vd2.setDefaultValue(ImageLayer.DEFAULT_BORDER_WIDTH);
        vd2.setDisplayName("Image border size");
        vd2.setDefaultConverter();
        addPropertyDescriptor(vd2);
*/
        return new JLabel("Figure style editor coming soon...");  // todo
    }

    @Override
    public void updateControl() {
        // todo
    }
}
