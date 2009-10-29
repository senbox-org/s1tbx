package org.esa.beam.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class GraticuleLayerType extends LayerType {

    public static final String PROPERTY_NAME_RASTER = "raster";
    public static final String PROPERTY_NAME_TRANSFORM = "imageToModelTransform";
    public static final String PROPERTY_NAME_RES_AUTO = "resAuto";
    public static final String PROPERTY_NAME_RES_PIXELS = "resPixels";
    public static final String PROPERTY_NAME_RES_LAT = "resLat";
    public static final String PROPERTY_NAME_RES_LON = "resLon";
    public static final String PROPERTY_NAME_LINE_COLOR = "lineColor";
    public static final String PROPERTY_NAME_LINE_TRANSPARENCY = "lineTransparency";
    public static final String PROPERTY_NAME_LINE_WIDTH = "lineWidth";
    public static final String PROPERTY_NAME_TEXT_ENABLED = "textEnabled";
    public static final String PROPERTY_NAME_TEXT_FONT = "textFont";
    public static final String PROPERTY_NAME_TEXT_FG_COLOR = "textFgColor";
    public static final String PROPERTY_NAME_TEXT_BG_COLOR = "textBgColor";
    public static final String PROPERTY_NAME_TEXT_BG_TRANSPARENCY = "textBgTransparency";
    public static final boolean DEFAULT_RES_AUTO = true;
    public static final int DEFAULT_RES_PIXELS = 128;
    public static final double DEFAULT_RES_LAT = 1.0;
    public static final double DEFAULT_RES_LON = 1.0;
    public static final Color DEFAULT_LINE_COLOR = new Color(204, 204, 255);
    public static final double DEFAULT_LINE_TRANSPARENCY = 0.0;
    public static final double DEFAULT_LINE_WIDTH = 0.5;
    public static final boolean DEFAULT_TEXT_ENABLED = true;
    public static final Font DEFAULT_TEXT_FONT = new Font("SansSerif", Font.ITALIC, 12);
    public static final Color DEFAULT_TEXT_FG_COLOR = Color.WHITE;
    public static final Color DEFAULT_TEXT_BG_COLOR = Color.BLACK;
    public static final double DEFAULT_TEXT_BG_TRANSPARENCY = 0.7;

    @Override
    public String getName() {
        return "Graticule Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        return new GraticuleLayer(this, (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER),
                                  configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();

        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addProperty(rasterModel);

        final Property transformModel = Property.create(PROPERTY_NAME_TRANSFORM, AffineTransform.class, new AffineTransform(), true);
        vc.addProperty(transformModel);

        final Property resAutoModel = Property.create(PROPERTY_NAME_RES_AUTO, Boolean.class, DEFAULT_RES_AUTO, true);
        vc.addProperty(resAutoModel);

        final Property resPixelsModel = Property.create(PROPERTY_NAME_RES_PIXELS, Integer.class, DEFAULT_RES_PIXELS, true);
        vc.addProperty(resPixelsModel);

        final Property resLatModel = Property.create(PROPERTY_NAME_RES_LAT, Double.class, DEFAULT_RES_LAT, true);
        vc.addProperty(resLatModel);

        final Property resLonModel = Property.create(PROPERTY_NAME_RES_LON, Double.class, DEFAULT_RES_LON, true);
        vc.addProperty(resLonModel);

        final Property lineColorModel = Property.create(PROPERTY_NAME_LINE_COLOR, Color.class, DEFAULT_LINE_COLOR, true);
        vc.addProperty(lineColorModel);

        final Property lineTransparencyModel = Property.create(PROPERTY_NAME_LINE_TRANSPARENCY, Double.class, DEFAULT_LINE_TRANSPARENCY, true);
        vc.addProperty(lineTransparencyModel);

        final Property lineWidthModel = Property.create(PROPERTY_NAME_LINE_WIDTH, Double.class, DEFAULT_LINE_WIDTH, true);
        vc.addProperty(lineWidthModel);

        final Property textEnabledModel = Property.create(PROPERTY_NAME_TEXT_ENABLED, Boolean.class, DEFAULT_TEXT_ENABLED, true);
        vc.addProperty(textEnabledModel);

        final Property textFontModel = Property.create(PROPERTY_NAME_TEXT_FONT, Font.class, DEFAULT_TEXT_FONT, true);
        vc.addProperty(textFontModel);

        final Property textFgColorModel = Property.create(PROPERTY_NAME_TEXT_FG_COLOR, Color.class, DEFAULT_TEXT_FG_COLOR, true);
        vc.addProperty(textFgColorModel);

        final Property textBgColorModel = Property.create(PROPERTY_NAME_TEXT_BG_COLOR, Color.class, DEFAULT_TEXT_BG_COLOR, true);
        vc.addProperty(textBgColorModel);

        final Property textBgTransparencyModel = Property.create(PROPERTY_NAME_TEXT_BG_TRANSPARENCY, Double.class, DEFAULT_TEXT_BG_TRANSPARENCY, true);
        vc.addProperty(textBgTransparencyModel);

        return vc;
    }
}
