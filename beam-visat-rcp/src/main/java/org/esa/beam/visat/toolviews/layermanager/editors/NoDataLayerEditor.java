package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.AbstractLayerConfigurationEditor;
import org.esa.beam.glayer.NoDataLayerType;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerEditor extends AbstractLayerConfigurationEditor {

    @Override
    protected void initializeBinding(AppContext appContext, final BindingContext bindingContext) {

        PropertyDescriptor vd = new PropertyDescriptor(NoDataLayerType.PROPERTY_NAME_COLOR, Color.class);
        vd.setDefaultValue(Color.ORANGE);
        vd.setDisplayName("No-data colour");
        vd.setDefaultConverter();

        addPropertyDescriptor(vd);
        bindingContext.getPropertySet().addPropertyChangeListener(NoDataLayerType.PROPERTY_NAME_COLOR,
                                                                     new UpdateImagePropertyChangeListener());
    }

    private class UpdateImagePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getLayer() != null) {
                final Color newColor = (Color) evt.getNewValue();
                final ImageLayer layer = (ImageLayer) getLayer();
                NoDataLayerType.renewMultiLevelSource(layer, newColor);
            }
        }

    }
}
