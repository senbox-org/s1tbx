package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerType extends ImageLayer.Type {

    public static final String NO_DATA_LAYER_ID = "org.esa.beam.layers.noData";
    public static final String PROPERTY_NAME_COLOR = "color";
    public static final String PROPERTY_NAME_TRANSPARENCY = "transparency";
    public static final String PROPERTY_NAME_RASTER = "raster";

    @Override
    public String getName() {
        return "No-Data Layer";
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
        Assert.notNull(color, PROPERTY_NAME_COLOR);
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);
        final AffineTransform i2mTransform = (AffineTransform) configuration.getValue(
                ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM);

        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            if (raster.getValidMaskExpression() != null) {
                multiLevelSource = MaskImageMultiLevelSource.create(raster.getProduct(), color,
                                                                    raster.getValidMaskExpression(), true,
                                                                    i2mTransform);
            } else {
                multiLevelSource = MultiLevelSource.NULL;
            }
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        }

        final ImageLayer noDataLayer;
        noDataLayer = new ImageLayer(this, multiLevelSource, configuration);
        noDataLayer.setName(getName());
        noDataLayer.setId(NO_DATA_LAYER_ID);
        noDataLayer.setVisible(false);
        return noDataLayer;
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer prototype = super.createLayerConfig(ctx);

        prototype.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        prototype.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);

        prototype.addProperty(Property.create(PROPERTY_NAME_COLOR, Color.class));
        prototype.getDescriptor(PROPERTY_NAME_COLOR).setNotNull(true);

        return prototype;

    }
}


