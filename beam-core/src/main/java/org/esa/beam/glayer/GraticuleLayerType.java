package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.DefaultStyle;
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
    protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new GraticuleLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = new ValueContainer();

        final ValueModel rasterModel = createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addModel(rasterModel);

        final ValueModel transformModel = createDefaultValueModel(PROPERTY_NAME_TRANSFORM, AffineTransform.class,
                                                                  new AffineTransform());
        vc.addModel(transformModel);

        final ValueModel resAutoModel = createDefaultValueModel(PROPERTY_NAME_RES_AUTO, Boolean.class,
                                                                DEFAULT_RES_AUTO);
        vc.addModel(resAutoModel);

        final ValueModel resPixelsModel = createDefaultValueModel(PROPERTY_NAME_RES_PIXELS, Integer.class,
                                                                  DEFAULT_RES_PIXELS);
        vc.addModel(resPixelsModel);

        final ValueModel resLatModel = createDefaultValueModel(PROPERTY_NAME_RES_LAT, Double.class, DEFAULT_RES_LAT);
        vc.addModel(resLatModel);

        final ValueModel resLonModel = createDefaultValueModel(PROPERTY_NAME_RES_LON, Double.class, DEFAULT_RES_LON);
        vc.addModel(resLonModel);

        final ValueModel lineColorModel = createDefaultValueModel(PROPERTY_NAME_LINE_COLOR, Color.class,
                                                                  DEFAULT_LINE_COLOR);
        vc.addModel(lineColorModel);

        final ValueModel lineTransparencyModel = createDefaultValueModel(PROPERTY_NAME_LINE_TRANSPARENCY,
                                                                         Double.class, DEFAULT_LINE_TRANSPARENCY);
        vc.addModel(lineTransparencyModel);

        final ValueModel lineWidthModel = createDefaultValueModel(PROPERTY_NAME_LINE_WIDTH, Double.class,
                                                                  DEFAULT_LINE_WIDTH);
        vc.addModel(lineWidthModel);

        final ValueModel textEnabledModel = createDefaultValueModel(PROPERTY_NAME_TEXT_ENABLED, Boolean.class,
                                                                    DEFAULT_TEXT_ENABLED);
        vc.addModel(textEnabledModel);

        final ValueModel textFontModel = createDefaultValueModel(PROPERTY_NAME_TEXT_FONT, Font.class,
                                                                 DEFAULT_TEXT_FONT);
        vc.addModel(textFontModel);

        final ValueModel textFgColorModel = createDefaultValueModel(PROPERTY_NAME_TEXT_FG_COLOR, Color.class,
                                                                    DEFAULT_TEXT_FG_COLOR);
        vc.addModel(textFgColorModel);

        final ValueModel textBgColorModel = createDefaultValueModel(PROPERTY_NAME_TEXT_BG_COLOR, Color.class,
                                                                    DEFAULT_TEXT_BG_COLOR);
        vc.addModel(textBgColorModel);

        final ValueModel textBgTransparencyModel = createDefaultValueModel(PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                                                                           Double.class, DEFAULT_TEXT_BG_TRANSPARENCY);
        vc.addModel(textBgTransparencyModel);

        return vc;
    }
}
