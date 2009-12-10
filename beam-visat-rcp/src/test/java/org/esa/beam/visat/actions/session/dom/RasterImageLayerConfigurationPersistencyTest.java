package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.glayer.RasterImageLayerType;

import java.awt.geom.AffineTransform;

public class RasterImageLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public RasterImageLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(RasterImageLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        final Band raster = getProductManager().getProduct(0).getBandAt(0);
        configuration.setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, raster);
        configuration.setValue("imageToModelTransform", new AffineTransform());
        return layerType.createLayer(null, configuration);
    }
}