package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.geom.AffineTransform;

public class RasterImageLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "Raster Data Layer";
    }

    @Override
    protected ImageLayer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
            final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                    ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
            final MultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(raster, i2mTransform,
                                                                                       ProgressMonitor.NULL);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }

        return new ImageLayer(this, configuration);
    }

    @Override
    public ValueContainer createLayerConfig(LayerContext ctx) {
        final ValueContainer template = super.createLayerConfig(ctx);

        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setItemAlias("raster");
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);

        return template;
    }

    public Layer createLayer(RasterDataNode raster, MultiLevelSource multiLevelSource) {
        final ValueContainer configuration = createLayerConfig(null);
        configuration.setValue(PROPERTY_NAME_RASTER, raster);
        if (multiLevelSource == null) {
            multiLevelSource = BandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM,
                               multiLevelSource.getModel().getImageToModelTransform(0));
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        return createLayer(null, configuration);
    }
}
