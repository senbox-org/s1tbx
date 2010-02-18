package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * The type descriptor of the {@link WindFieldLayer}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayerType extends LayerType {

    private static final String TYPE_NAME = "WindFieldLayerType";
    private static final String[] ALIASES = {"org.esa.beam.visat.toolviews.layermanager.layersrc.windfield.WindFieldLayerType"};
    
    public static WindFieldLayer createLayer(RasterDataNode windu, RasterDataNode windv) {
        LayerType type = LayerTypeRegistry.getLayerType(WindFieldLayerType.class);
        final PropertySet template = type.createLayerConfig(null);
        template.setValue("windu", windu);
        template.setValue("windv", windv);
        return new WindFieldLayer(type, windu, windv, template);
    }

    @Override
    public String getName() {
        return TYPE_NAME;
    }
    
    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        // todo - need to check for availability of windu, windv  (nf)
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final RasterDataNode windu = (RasterDataNode) configuration.getValue("windu");
        final RasterDataNode windv = (RasterDataNode) configuration.getValue("windv");
        return new WindFieldLayer(this, windu, windv, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer propertyContainer = new PropertyContainer();
        // todo - how do I know whether my value model type can be serialized or not? (nf)
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windu", RasterDataNode.class), new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windv", RasterDataNode.class), new DefaultPropertyAccessor()));
        return propertyContainer;
    }
}
