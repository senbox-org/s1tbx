package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.glayer.Style;
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
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerEditor extends AbstractValueDescriptorLayerEditor {

    @Override
    protected void collectValueDescriptors(AppContext appContext, final List<ValueDescriptor> descriptorList) {

        ValueDescriptor vd = new ValueDescriptor(NoDataLayerType.PROPERTY_COLOR, Color.class);
        vd.setDefaultValue(Color.ORANGE);
        vd.setDisplayName("No-Data Colour");
        vd.setDefaultConverter();

        descriptorList.add(vd);
    }

    @Override
    protected void collectPropertyChangeListeners(List<PropertyChangeListener> listenerList) {
        super.collectPropertyChangeListeners(listenerList);

        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (getLayer() != null && NoDataLayerType.PROPERTY_COLOR.equals(evt.getPropertyName())) {
                    final ImageLayer layer = (ImageLayer) getLayer();
                    final Style style = layer.getStyle();
                    final Color newColor = (Color) evt.getNewValue();
                    final RasterDataNode raster = (RasterDataNode) style.getProperty(
                            NoDataLayerType.PROPERTY_REFERENCED_RASTER);
                    final AffineTransform transform = (AffineTransform) style.getProperty(
                            NoDataLayerType.PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
                    MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(raster.getProduct(),
                                                                                         newColor,
                                                                                         raster.getValidMaskExpression(),
                                                                                         true,
                                                                                         transform);

                    layer.setMultiLevelSource(multiLevelSource);
                }
            }
        };
        listenerList.add(listener);
    }
}
