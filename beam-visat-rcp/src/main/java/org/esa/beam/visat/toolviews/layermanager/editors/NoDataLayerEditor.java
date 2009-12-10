package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.NoDataLayerType;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerEditor extends AbstractBindingLayerEditor {

    @Override
    protected void initializeBinding(AppContext appContext, final BindingContext bindingContext) {

        PropertyDescriptor vd = new PropertyDescriptor(NoDataLayerType.PROPERTY_NAME_COLOR, Color.class);
        vd.setDefaultValue(Color.ORANGE);
        vd.setDisplayName("No-data colour");
        vd.setDefaultConverter();

        addValueDescriptor(vd);
        bindingContext.getPropertySet().addPropertyChangeListener(NoDataLayerType.PROPERTY_NAME_COLOR,
                                                                     new UpdateImagePropertyChangeListener());
    }

    private class UpdateImagePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getLayer() != null) {
                final ImageLayer layer = (ImageLayer) getLayer();
                final PropertySet configuration = layer.getConfiguration();
                final Color newColor = (Color) evt.getNewValue();
                final RasterDataNode raster = (RasterDataNode) configuration.getValue(
                        NoDataLayerType.PROPERTY_NAME_RASTER);
                final AffineTransform transform = (AffineTransform) configuration.getValue(
                        ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
                MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(raster.getProduct(),
                                                                                     newColor,
                                                                                     raster.getValidMaskExpression(),
                                                                                     true,
                                                                                     transform);

                layer.setMultiLevelSource(multiLevelSource);
            }
        }
    }
}
