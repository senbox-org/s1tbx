package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * The type descriptor of the {@link WindFieldLayer}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayerType extends LayerType {
    // todo - weird!!!
    // todo - got NPE, although SPI registered
    //   static LayerType instance = getLayerType(WindFieldLayerType.class.getName());
    static LayerType instance = new WindFieldLayerType();

    public static WindFieldLayer createLayer(RasterDataNode windu, RasterDataNode windv) {
        final ValueContainer template = instance.createLayerConfig(null);
        template.setValue("windu", windu);
        template.setValue("windv", windv);
        return new WindFieldLayer(template);
    }

    @Override
    public String getName() {
        return "Wind Speed";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        // todo - need to check for availability of windu, windv  (nf)
        return true;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new WindFieldLayer(configuration);
    }

    // todo - rename getDefaultConfiguration  ? (nf)
    @Override
    public ValueContainer createLayerConfig(LayerContext ctx) {
        final ValueContainer valueContainer = new ValueContainer();
        // todo - how do I know whether my value model type can be serialized or not? (nf)
        valueContainer.addModel(new ValueModel(new ValueDescriptor("windu", RasterDataNode.class), new DefaultValueAccessor()));
        valueContainer.addModel(new ValueModel(new ValueDescriptor("windv", RasterDataNode.class), new DefaultValueAccessor()));
        return valueContainer;
    }
}
