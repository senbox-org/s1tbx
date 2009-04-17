package org.esa.beam.glayer;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.binding.ValueContainer;

public class RasterImageLayerType extends LayerType {

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ValueContainer createConfiguration(LayerContext ctx, Layer layer) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
