package org.esa.beam.glayer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.RoiImageMultiLevelSource;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Marco Peters
 * @version $ Revision: $ Date: $
 * @since BEAM 4.6
 */
public class RoiLayerType extends LayerType {

    public static final String ROI_LAYER_ID = "org.esa.beam.layers.roi";
    public static final String PROPERTY_COLOR = "roiOverlay.color";
    public static final String PROPERTY_TRANSPARENCY = "roiOverlay.transparency";
    public static final String PROPERTY_REFERENCED_RASTER = "roiOverlay.referencedRaster";
    public static final String PROPERTY_IMAGE_TO_MODEL_TRANSFORM = "roiOverlay.imageToModelTransform";

    @Override
    public String getName() {
        return "ROI";
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

        if (raster.getROIDefinition() != null && raster.getROIDefinition().isUsable()) {
            multiLevelSource = RoiImageMultiLevelSource.create(raster, color, i2mTransform);
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer roiLayer = new ImageLayer(this, multiLevelSource);
        roiLayer.setName("ROI");
        roiLayer.setId(ROI_LAYER_ID);
        roiLayer.setVisible(false);
        configureLayer(configuration, roiLayer);

        return roiLayer;
    }

    private void configureLayer(Map<String, Object> configuration, Layer layer) {

        Color color = (Color) configuration.get(PROPERTY_COLOR);
        Double transparency = (Double) configuration.get(PROPERTY_TRANSPARENCY);
        RasterDataNode raster = (RasterDataNode) configuration.get(PROPERTY_REFERENCED_RASTER);
        AffineTransform i2mTransform = (AffineTransform) configuration.get(PROPERTY_IMAGE_TO_MODEL_TRANSFORM);
        if (transparency == null) {
            transparency = 0.0;
        }
        Style style = layer.getStyle();
        style.setProperty(PROPERTY_COLOR, color);
        style.setOpacity(1.0 - transparency);
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