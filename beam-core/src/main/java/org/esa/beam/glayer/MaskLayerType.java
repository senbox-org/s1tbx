package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.operator.MultiplyConstDescriptor;
import java.awt.Color;
import java.awt.image.RenderedImage;


/**
 * A layer used to display {@link Mask}s.
 *
 * @author Norman Fomferra
 * @version $ Revision: $ Date: $
 * @since BEAM 4.7
 */
public class MaskLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_MASK = "mask";

    public static Layer createLayer(RasterDataNode raster, Mask mask) {
        final MaskLayerType type = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        final PropertySet configuration = type.createLayerConfig(null);
        configuration.setValue(MaskLayerType.PROPERTY_NAME_MASK, mask);

        final Layer layer = type.createLayer(null, configuration);
        layer.setVisible(raster.getOverlayMaskGroup().contains(mask));

        return layer;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            multiLevelSource = createMultiLevelSource(configuration);
        }
        configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        final ImageLayer layer = new ImageLayer(this, multiLevelSource, configuration);
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        layer.setName(mask.getName());

        return layer;
    }

    public static MultiLevelSource createMultiLevelSource(PropertySet configuration) {
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        return createMultiLevelSource(mask);
    }

    public static MultiLevelSource createMultiLevelSource(final Mask mask) {
        return new AbstractMultiLevelSource(mask.getSourceImage().getModel()) {
            @Override
            protected RenderedImage createImage(int level) {
                final Color color = mask.getImageColor();
                final double opacity = 1.0 - mask.getImageTransparency();
                final RenderedImage levelImage = mask.getSourceImage().getImage(level);
                final RenderedImage alphaImage = createAlphaImage(levelImage, opacity);

                return ImageManager.createColoredMaskImage(color, alphaImage);
            }

            private RenderedImage createAlphaImage(RenderedImage levelImage, double opacity) {
                return MultiplyConstDescriptor.create(levelImage, new double[]{opacity}, null);
            }
        };
    }


    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet layerConfig = super.createLayerConfig(ctx);

        layerConfig.addProperty(Property.create(PROPERTY_NAME_MASK, Mask.class));
        layerConfig.getProperty(PROPERTY_NAME_MASK).getDescriptor().setNotNull(true);

        return layerConfig;
    }
}