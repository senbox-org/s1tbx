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

    public static final String PROPERTY_NAME_RASTER = "graticule.raster";
    public static final String PROPERTY_NAME_TRANSFORM = "graticule.i2mTransform";
    public static final String PROPERTY_NAME_STYLE = "graticule.style";
    public static final String PROPERTY_NAME_RES_AUTO = "graticule.res.auto";
    public static final String PROPERTY_NAME_RES_PIXELS = "graticule.res.pixels";
    public static final String PROPERTY_NAME_RES_LAT = "graticule.res.lat";
    public static final String PROPERTY_NAME_RES_LON = "graticule.res.lon";
    public static final String PROPERTY_NAME_LINE_COLOR = "graticule.line.color";
    public static final String PROPERTY_NAME_LINE_TRANSPARENCY = "graticule.line.transparency";
    public static final String PROPERTY_NAME_LINE_WIDTH = "graticule.line.width";
    public static final String PROPERTY_NAME_TEXT_ENABLED = "graticule.text.enabled";
    public static final String PROPERTY_NAME_TEXT_FONT = "graticule.text.font";
    public static final String PROPERTY_NAME_TEXT_FG_COLOR = "graticule.text.fg.color";
    public static final String PROPERTY_NAME_TEXT_BG_COLOR = "graticule.text.bg.color";
    public static final String PROPERTY_NAME_TEXT_BG_TRANSPARENCY = "graticule.text.bg.transparency";
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
        final GraticuleLayer layer = new GraticuleLayer(this, configuration);
        final DefaultStyle style = new DefaultStyle();
        style.setProperty(PROPERTY_NAME_RES_AUTO, configuration.getValue(PROPERTY_NAME_RES_AUTO));
        style.setProperty(PROPERTY_NAME_RES_PIXELS, configuration.getValue(PROPERTY_NAME_RES_PIXELS));
        style.setProperty(PROPERTY_NAME_RES_LAT, configuration.getValue(PROPERTY_NAME_RES_LAT));
        style.setProperty(PROPERTY_NAME_RES_LON, configuration.getValue(PROPERTY_NAME_RES_LON));
        style.setProperty(PROPERTY_NAME_LINE_COLOR, configuration.getValue(PROPERTY_NAME_LINE_COLOR));
        style.setProperty(PROPERTY_NAME_LINE_TRANSPARENCY, configuration.getValue(PROPERTY_NAME_LINE_TRANSPARENCY));
        style.setProperty(PROPERTY_NAME_LINE_WIDTH, configuration.getValue(PROPERTY_NAME_LINE_WIDTH));
        style.setProperty(PROPERTY_NAME_TEXT_ENABLED, configuration.getValue(PROPERTY_NAME_TEXT_ENABLED));
        style.setProperty(PROPERTY_NAME_TEXT_FONT, configuration.getValue(PROPERTY_NAME_TEXT_FONT));
        style.setProperty(PROPERTY_NAME_TEXT_FG_COLOR, configuration.getValue(PROPERTY_NAME_TEXT_FG_COLOR));
        style.setProperty(PROPERTY_NAME_TEXT_BG_COLOR, configuration.getValue(PROPERTY_NAME_TEXT_BG_COLOR));
        style.setProperty(PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                          configuration.getValue(PROPERTY_NAME_TEXT_BG_TRANSPARENCY));
        layer.setStyle(style);
        return layer;

    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer vc = new ValueContainer();

        final ValueModel rasterModel = createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addModel(rasterModel);

        final ValueModel transformModel = createDefaultValueModel(PROPERTY_NAME_TRANSFORM, new AffineTransform());
        transformModel.getDescriptor().setDefaultValue(new AffineTransform());
        vc.addModel(transformModel);

        final ValueModel resAutoModel = createDefaultValueModel(PROPERTY_NAME_RES_AUTO, DEFAULT_RES_AUTO);
        resAutoModel.getDescriptor().setDefaultValue(DEFAULT_RES_AUTO);
        vc.addModel(resAutoModel);

        final ValueModel resPixelsModel = createDefaultValueModel(PROPERTY_NAME_RES_PIXELS, DEFAULT_RES_PIXELS);
        resPixelsModel.getDescriptor().setDefaultValue(DEFAULT_RES_PIXELS);
        vc.addModel(resPixelsModel);

        final ValueModel resLatModel = createDefaultValueModel(PROPERTY_NAME_RES_LAT, DEFAULT_RES_LAT);
        resLatModel.getDescriptor().setDefaultValue(DEFAULT_RES_LAT);
        vc.addModel(resLatModel);

        final ValueModel resLonModel = createDefaultValueModel(PROPERTY_NAME_RES_LON, DEFAULT_RES_LON);
        resLonModel.getDescriptor().setDefaultValue(DEFAULT_RES_LON);
        vc.addModel(resLonModel);

        final ValueModel lineColorModel = createDefaultValueModel(PROPERTY_NAME_LINE_COLOR, DEFAULT_LINE_COLOR);
        lineColorModel.getDescriptor().setDefaultValue(DEFAULT_LINE_COLOR);
        vc.addModel(lineColorModel);

        final ValueModel lineTransparencyModel = createDefaultValueModel(PROPERTY_NAME_LINE_TRANSPARENCY,
                                                                         DEFAULT_LINE_TRANSPARENCY);
        lineTransparencyModel.getDescriptor().setDefaultValue(DEFAULT_LINE_TRANSPARENCY);
        vc.addModel(lineTransparencyModel);

        final ValueModel lineWidthModel = createDefaultValueModel(PROPERTY_NAME_LINE_WIDTH, DEFAULT_LINE_WIDTH);
        lineWidthModel.getDescriptor().setDefaultValue(DEFAULT_LINE_WIDTH);
        vc.addModel(lineWidthModel);

        final ValueModel textEnabledModel = createDefaultValueModel(PROPERTY_NAME_TEXT_ENABLED, DEFAULT_TEXT_ENABLED);
        textEnabledModel.getDescriptor().setDefaultValue(DEFAULT_TEXT_ENABLED);
        vc.addModel(textEnabledModel);

        final ValueModel textFontModel = createDefaultValueModel(PROPERTY_NAME_TEXT_FONT, DEFAULT_TEXT_FONT);
        textFontModel.getDescriptor().setDefaultValue(DEFAULT_TEXT_FONT);
        vc.addModel(textFontModel);

        final ValueModel textFgColorModel = createDefaultValueModel(PROPERTY_NAME_TEXT_FG_COLOR, DEFAULT_TEXT_FG_COLOR);
        textFgColorModel.getDescriptor().setDefaultValue(DEFAULT_TEXT_FG_COLOR);
        vc.addModel(textFgColorModel);

        final ValueModel textBgColorModel = createDefaultValueModel(PROPERTY_NAME_TEXT_BG_COLOR, DEFAULT_TEXT_BG_COLOR);
        textBgColorModel.getDescriptor().setDefaultValue(DEFAULT_TEXT_BG_COLOR);
        vc.addModel(textBgColorModel);

        final ValueModel textBgTransparencyModel = createDefaultValueModel(PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                                                                           DEFAULT_TEXT_BG_TRANSPARENCY);
        textBgTransparencyModel.getDescriptor().setDefaultValue(DEFAULT_TEXT_BG_TRANSPARENCY);
        vc.addModel(textBgTransparencyModel);

        return vc;
    }
}
