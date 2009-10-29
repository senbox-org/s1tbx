package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
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
        final PropertyContainer template = instance.createLayerConfig(null);
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
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        return new WindFieldLayer(configuration);
    }

    // todo - rename getDefaultConfiguration  ? (nf)
    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer propertyContainer = new PropertyContainer();
        // todo - how do I know whether my value model type can be serialized or not? (nf)
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windu", RasterDataNode.class), new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windv", RasterDataNode.class), new DefaultPropertyAccessor()));
        return propertyContainer;
    }
}
