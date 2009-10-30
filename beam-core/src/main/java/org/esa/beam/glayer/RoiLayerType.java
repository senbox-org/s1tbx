package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.RoiImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;


/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class RoiLayerType extends ImageLayer.Type {

    public static final String ROI_LAYER_ID = "org.esa.beam.layers.roi";
    public static final String PROPERTY_NAME_COLOR = "color";
    public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "ROI";
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);

        MultiLevelSource multiLevelSource;
        multiLevelSource = (MultiLevelSource) configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            if (raster.getROIDefinition() != null && raster.getROIDefinition().isUsable()) {
                final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
                final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                        ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);
                multiLevelSource = RoiImageMultiLevelSource.create(raster, color, i2mTransform);
            } else {
                multiLevelSource = MultiLevelSource.NULL;
            }
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }

        final ImageLayer roiLayer = new ImageLayer(this, multiLevelSource, configuration);
        roiLayer.setName("ROI");
        roiLayer.setId(ROI_LAYER_ID);

        return roiLayer;
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer prototype = super.createLayerConfig(ctx);

        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        prototype.addProperty(rasterModel);

        prototype.addProperty(Property.create(PROPERTY_NAME_COLOR, Color.class, Color.RED, true));
        prototype.addProperty(Property.create(PROPERTY_NAME_TRANSPARENCY, Double.class, 0.5, true));

        return prototype;
    }
}