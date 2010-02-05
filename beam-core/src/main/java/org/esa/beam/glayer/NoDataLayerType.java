package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
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
 * A layer used to display the no-data mask of a raster data node.
 *
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerType extends ImageLayer.Type {

    public static final String NO_DATA_LAYER_ID = "org.esa.beam.layers.noData";
    public static final String PROPERTY_NAME_COLOR = "color";
    public static final String PROPERTY_NAME_RASTER = "raster";
    public static final Color DEFAULT_COLOR = Color.ORANGE;

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final Color color = (Color) configuration.getValue(PROPERTY_NAME_COLOR);
        Assert.notNull(color, PROPERTY_NAME_COLOR);
        final RasterDataNode raster = (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER);

        MultiLevelSource multiLevelSource = (MultiLevelSource) configuration.getValue(
                ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        if (multiLevelSource == null) {
            if (raster.getValidMaskExpression() != null) {
                final AffineTransform i2mTransform = raster.getSourceImage().getModel().getImageToModelTransform(0);
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
        noDataLayer.setName("No-Data Layer");
        noDataLayer.setId(NO_DATA_LAYER_ID);
        noDataLayer.setVisible(false);
        return noDataLayer;
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertySet prototype = super.createLayerConfig(ctx);

        prototype.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        prototype.getDescriptor(PROPERTY_NAME_RASTER).setNotNull(true);

        prototype.addProperty(Property.create(PROPERTY_NAME_COLOR, Color.class, DEFAULT_COLOR, true));

        return prototype;

    }
}


