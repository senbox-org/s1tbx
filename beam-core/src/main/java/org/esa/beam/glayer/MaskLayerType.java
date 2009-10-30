package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.Color;
import java.awt.image.RenderedImage;


/**
 * A layer used to display {@link Mask}s.
 * @author Norman Fomferra
 * @version $ Revision: $ Date: $
 * @since BEAM 4.7
 */
public class MaskLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_MASK = "mask";

    @Override
    public String getName() {
        return "Mask Layer";
    }

    public static Layer createLayer(RasterDataNode raster, Mask mask) {
        MaskLayerType type = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        PropertyContainer configuration = type.createLayerConfig(null);
        configuration.setValue(MaskLayerType.PROPERTY_NAME_MASK, mask);
        Layer layer = type.createLayer(null, configuration);
        layer.setVisible(raster.getOverlayMaskGroup().contains(mask));
        return layer;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        MultiLevelSource multiLevelSource = (MultiLevelSource)configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            multiLevelSource = createMultiLevelSource(configuration);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        final ImageLayer layer = new ImageLayer(this, multiLevelSource, configuration);
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        layer.setName(mask.getName());
        layer.setTransparency((Double) mask.getImageConfig().getValue("transparency"));
        return layer;
    }

    public static MultiLevelSource createMultiLevelSource(PropertyContainer configuration) {
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        return createMultiLevelSource(mask);
    }

    public static MultiLevelSource createMultiLevelSource(final Mask mask) {
        return new AbstractMultiLevelSource(mask.getSourceImage().getModel()) {
            @Override
            protected RenderedImage createImage(int level) {
                final Color color = (Color) mask.getImageConfig().getValue("color");
                RenderedImage maskImage = mask.getSourceImage().getImage(level);
                return ImageManager.createColoredMaskImage(color, maskImage);
            }
        };
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = super.createLayerConfig(ctx);

        vc.addProperty(Property.create(PROPERTY_NAME_MASK, Mask.class));
        vc.getProperty(PROPERTY_NAME_MASK).getDescriptor().setNotNull(true);

        return vc;
    }
}