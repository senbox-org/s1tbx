package org.esa.beam.glayer;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.binding.ValueContainer;

public class RasterImageLayerType extends LayerType {

    @Override
    public String getName() {
        return "Raster Data Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        return null;
    }

    @Override
    public ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer) {
        return null;
    }
}
