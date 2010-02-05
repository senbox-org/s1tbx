package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
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
    public ImageLayer createLayer(LayerContext ctx, PropertySet configuration) {
        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
            final AffineTransform i2mTransform = raster.getSourceImage().getModel().getImageToModelTransform(0);
            multiLevelSource = BandImageMultiLevelSource.create(raster, i2mTransform, ProgressMonitor.NULL);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }
        return new ImageLayer(this, multiLevelSource, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet template = super.createLayerConfig(ctx);

        template.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.getDescriptor(PROPERTY_NAME_RASTER).setItemAlias("raster");
        template.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);

        return template;
    }

    public Layer createLayer(RasterDataNode raster, MultiLevelSource multiLevelSource) {
        final PropertySet configuration = createLayerConfig(null);
        configuration.setValue(PROPERTY_NAME_RASTER, raster);
        if (multiLevelSource == null) {
            multiLevelSource = BandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        return createLayer(null, configuration);
    }
}
