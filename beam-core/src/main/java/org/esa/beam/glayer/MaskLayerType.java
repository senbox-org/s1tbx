package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Mask;
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
        MaskLayerType type = LayerType.getLayerType(MaskLayerType.class);
        ValueContainer configuration = type.createLayerConfig(null);
        configuration.setValue(MaskLayerType.PROPERTY_NAME_MASK, mask);
        Layer layer = type.createLayer(null, configuration);
        layer.setVisible(raster.getOverlayMaskGroup().contains(mask));
        return layer;
    }

    @Override
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final MultiLevelSource multiLevelSource = createMultiLevelSource(configuration);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM) == null) {
            //final MultiLevelSource multiLevelSource =
            // configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }
        final ImageLayer layer = new ImageLayer(this, configuration);
        final Mask mask = (Mask) configuration.getValue(PROPERTY_NAME_MASK);
        layer.setName(mask.getName());
        layer.setTransparency((Double) mask.getImageConfig().getValue("transparency"));
        return layer;
    }

    public static MultiLevelSource createMultiLevelSource(ValueContainer configuration) {
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
    public ValueContainer createLayerConfig(LayerContext ctx) {
        final ValueContainer vc = super.createLayerConfig(ctx);

        vc.addModel(createDefaultValueModel(PROPERTY_NAME_MASK, Mask.class));
        vc.getModel(PROPERTY_NAME_MASK).getDescriptor().setNotNull(true);

        return vc;
    }
}