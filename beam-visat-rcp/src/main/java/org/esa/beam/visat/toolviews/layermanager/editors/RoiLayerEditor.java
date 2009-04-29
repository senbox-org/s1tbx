package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.RoiLayerType;
import org.esa.beam.glevel.RoiImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class RoiLayerEditor extends AbstractBindingLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, final BindingContext bindingContext) {
        ValueDescriptor vd = new ValueDescriptor(RoiLayerType.PROPERTY_COLOR, Color.class);
        vd.setDefaultValue(Color.RED);
        vd.setDisplayName("Roi Colour");
        vd.setDefaultConverter();
        addValueDescriptor(vd);

        bindingContext.getValueContainer().addPropertyChangeListener(RoiLayerType.PROPERTY_COLOR,
                                                                     new UpdateImagePropertyChangeListener());
    }

    private class UpdateImagePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getLayer() != null && RoiLayerType.PROPERTY_COLOR.equals(evt.getPropertyName())) {
                final ImageLayer layer = (ImageLayer) getLayer();
                final Style style = layer.getStyle();
                final Color newColor = (Color) evt.getNewValue();
                final RasterDataNode raster = (RasterDataNode) style.getProperty(
                        RoiLayerType.PROPERTY_REFERENCED_RASTER);
                final AffineTransform transform = (AffineTransform) style.getProperty(
                        ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
                MultiLevelSource multiLevelSource = RoiImageMultiLevelSource.create(raster, newColor, transform);

                layer.setMultiLevelSource(multiLevelSource);
            }
        }
    }
}