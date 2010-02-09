package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Mask.ImageType;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Color;

/**
 * @deprecated since BEAM 4.7, replaced by MaskLayerType
 */
@Deprecated
public class RoiLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_COLOR = ImageType.PROPERTY_NAME_COLOR;
    public static final String PROPERTY_NAME_TRANSPARENCY = ImageType.PROPERTY_NAME_TRANSPARENCY;
    public static final String PROPERTY_NAME_RASTER = "raster";

    private static final String TYPE_NAME = "RoiLayerType";
    private static final String[] ALIASES = {"org.esa.beam.glayer.RoiLayerType"};

    @Override
    public String getName() {
        return TYPE_NAME;
    }
    
    @Override
    public String[] getAliases() {
        return ALIASES;
    }
    
    /**
     * Converts a RoiLayer into a MaskLayer, for backward compatibility.
     */
    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
        String maskName = raster.getName() + "_roi";
        final Mask mask = raster.getProduct().getMaskGroup().get(maskName);
        if (mask == null) {
            throw new IllegalArgumentException("Mask '" + maskName + "'not available in product.");
        }
        final double transparency = (Double) configuration.getValue(PROPERTY_NAME_TRANSPARENCY);
        mask.setImageTransparency(transparency);
        final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
        mask.setImageColor(color);

        MaskLayerType maskLayerType = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        PropertySet maskConfiguration = maskLayerType.createLayerConfig(null);
        for (Property property : maskConfiguration.getProperties()) {
            String propertyName = property.getName();
            Property srcProperty = configuration.getProperty(propertyName);
            if (srcProperty != null) {
                maskConfiguration.setValue(propertyName, srcProperty.getValue());
            }
        }
        maskConfiguration.setValue(MaskLayerType.PROPERTY_NAME_MASK, mask);
        return maskLayerType.createLayer(ctx, maskConfiguration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet prototype = super.createLayerConfig(ctx);

        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        prototype.addProperty(rasterModel);

        prototype.addProperty(Property.create(PROPERTY_NAME_COLOR, Color.class, Color.RED, true));
        prototype.addProperty(Property.create(PROPERTY_NAME_TRANSPARENCY, Double.class, 0.5, true));

        return prototype;
    }
}