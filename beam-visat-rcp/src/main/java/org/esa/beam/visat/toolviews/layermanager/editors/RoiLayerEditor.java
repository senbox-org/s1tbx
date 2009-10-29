package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.ROIDefinition;
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
        PropertyDescriptor vd = new PropertyDescriptor(RoiLayerType.PROPERTY_NAME_COLOR, Color.class);
        vd.setDefaultValue(Color.RED);
        vd.setDisplayName("ROI colour");
        vd.setDefaultConverter();
        addValueDescriptor(vd);

        bindingContext.getPropertyContainer().addPropertyChangeListener(RoiLayerType.PROPERTY_NAME_COLOR,
                                                                     new UpdateImagePropertyChangeListener());
    }

    private class UpdateImagePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getLayer() != null && RoiLayerType.PROPERTY_NAME_COLOR.equals(evt.getPropertyName())) {
                final ImageLayer layer = (ImageLayer) getLayer();
                final PropertyContainer configuration = layer.getConfiguration();
                final Color newColor = (Color) evt.getNewValue();
                final RasterDataNode raster = (RasterDataNode) configuration.getValue(
                        RoiLayerType.PROPERTY_NAME_RASTER);
                final AffineTransform transform = (AffineTransform) configuration.getValue(
                        ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
                final ROIDefinition definition = raster.getROIDefinition();
                MultiLevelSource multiLevelSource;
                if (definition != null && definition.isUsable()) {
                    multiLevelSource = RoiImageMultiLevelSource.create(raster, newColor, transform);
                } else {
                    multiLevelSource = MultiLevelSource.NULL;
                }
                layer.setMultiLevelSource(multiLevelSource);
            }
        }
    }
}