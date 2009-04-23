package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.glayer.FigureLayerType;

public class FigureLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public FigureLayerConfigurationPersistencyTest() {
        super(LayerType.getLayerType(FigureLayerType.class.getName()));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final ValueContainer configuration = layerType.getConfigurationTemplate();
        // TODO - configure
        return layerType.createLayer(null, configuration);
    }

    @Override
    public void testLayerConfigurationPersistency() throws Exception {
        // TODO - remove
    }
}
