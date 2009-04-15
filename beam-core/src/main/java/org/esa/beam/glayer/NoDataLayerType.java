package org.esa.beam.glayer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.MaskImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class NoDataLayerType extends LayerType {

    public static final String NO_DATA_LAYER_ID = "org.esa.beam.layers.noData";
    public static final String PROPERTY_COLOR = "noDataOverlay.color";
    public static final String PROPERTY_TRANSPARENCY = "noDataOverlay.transparency";
    public static final String PROPERTY_REFERENCED_RASTER = "noDataOverlay.referencedRaster";
    public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "noDataOverlay.imageToModelTransform";

    @Override
    public String getName() {
        return "No-Data Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
        final MultiLevelSource multiLevelSource;
        final Color color = (Color) configuration.get(PROPERTY_COLOR);
        Assert.notNull(color, PROPERTY_COLOR);
        final RasterDataNode raster = (RasterDataNode) configuration.get(PROPERTY_REFERENCED_RASTER);
        Assert.notNull(raster, PROPERTY_REFERENCED_RASTER);
        final AffineTransform i2mTransform = (AffineTransform) configuration.get(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
        Assert.notNull(i2mTransform, PROPERTY_IMAGE_TO_MODEL_TRANSFORM);

        if (raster.getValidMaskExpression() != null) {
            multiLevelSource = MaskImageMultiLevelSource.create(raster.getProduct(), color,
                                                                raster.getValidMaskExpression(), true,
                                                                i2mTransform);
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }
        final ImageLayer noDataLayer = new ImageLayer(this, multiLevelSource);
        noDataLayer.setName(getName());
        noDataLayer.setId(NO_DATA_LAYER_ID);
        noDataLayer.setVisible(false);
        configureLayer(configuration, noDataLayer);
        return noDataLayer;
    }

    private void configureLayer(Map<String, Object> configuration, Layer layer) {
        final Color color = (Color) configuration.get(PROPERTY_COLOR);
        Double transparency = (Double) configuration.get(PROPERTY_TRANSPARENCY);
        final RasterDataNode raster = (RasterDataNode) configuration.get(PROPERTY_REFERENCED_RASTER);
        final AffineTransform i2mTransform = (AffineTransform) configuration.get(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
        if (transparency == null) {
            transparency = 0.0;
        }
        final Style style = layer.getStyle();
        style.setOpacity(1.0 - transparency);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
        style.setProperty(PROPERTY_COLOR, color);
        style.setProperty(PROPERTY_REFERENCED_RASTER, raster);
        style.setProperty(PROPERTY_IMAGE_TO_MODEL_TRANSFORM, i2mTransform);

        style.setComposite(layer.getStyle().getComposite());
        layer.setStyle(style);
    }


    @Override
    public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
        final HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("multiLevelSource", ((ImageLayer) layer).getMultiLevelSource());
        return configuration;
    }
}


