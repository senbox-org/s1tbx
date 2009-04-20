package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.BandImageMultiLevelSource;

public class RasterImageLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_RASTERS = "rasters";

    @Override
    public String getName() {
        return "Raster Data Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public ImageLayer createLayer(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final RasterDataNode[] rasters = (RasterDataNode[]) configuration.getValue(PROPERTY_NAME_RASTERS);
            MultiLevelSource levelSource = BandImageMultiLevelSource.create(rasters, ProgressMonitor.NULL);
            try {
                configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, levelSource);
            } catch (ValidationException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return new ImageLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = super.getConfigurationTemplate();

        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTERS, RasterDataNode[].class));
        template.getDescriptor(PROPERTY_NAME_RASTERS).setNotNull(true);

        return template;
    }

    public ImageLayer createLayer(RasterDataNode[] rasters, MultiLevelSource levelSource) {
        final ValueContainer configuration = getConfigurationTemplate();

        try {
            configuration.setValue(PROPERTY_NAME_RASTERS, rasters);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, levelSource);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return createLayer(null, configuration);
    }
}
