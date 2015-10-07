/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;


@LayerTypeMetadata(name = "GraticuleLayerType", aliasNames = {"org.esa.snap.core.layer.GraticuleLayerType"})
public class GraticuleLayerType extends LayerType {

    public static final String PROPERTY_NAME_RASTER = "raster";
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

    private static final String ALIAS_NAME_RES_AUTO = "resAuto";
    private static final String ALIAS_NAME_RES_PIXELS = "resPixels";
    private static final String ALIAS_NAME_RES_LAT = "resLat";
    private static final String ALIAS_NAME_RES_LON = "resLon";
    private static final String ALIAS_NAME_LINE_COLOR = "lineColor";
    private static final String ALIAS_NAME_LINE_TRANSPARENCY = "lineTransparency";
    private static final String ALIAS_NAME_LINE_WIDTH = "lineWidth";
    private static final String ALIAS_NAME_TEXT_ENABLED = "textEnabled";
    private static final String ALIAS_NAME_TEXT_FONT = "textFont";
    private static final String ALIAS_NAME_TEXT_FG_COLOR = "textFgColor";
    private static final String ALIAS_NAME_TEXT_BG_COLOR = "textBgColor";
    private static final String ALIAS_NAME_TEXT_BG_TRANSPARENCY = "textBgTransparency";

    /**
     * @deprecated since BEAM 4.7, no replacement; kept for compatibility of sessions
     */
    @Deprecated
    private static final String PROPERTY_NAME_TRANSFORM = "imageToModelTransform";


    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        return new GraticuleLayer(this, (RasterDataNode) configuration.getValue(PROPERTY_NAME_RASTER),
                                  configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer vc = new PropertyContainer();

        final Property rasterModel = Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class);
        rasterModel.getDescriptor().setNotNull(true);
        vc.addProperty(rasterModel);
         final Property transformModel = Property.create(PROPERTY_NAME_TRANSFORM, new AffineTransform());
        transformModel.getDescriptor().setTransient(true);
        vc.addProperty(transformModel);

        final Property resAutoModel = Property.create(PROPERTY_NAME_RES_AUTO, Boolean.class, DEFAULT_RES_AUTO, true);
        resAutoModel.getDescriptor().setAlias(ALIAS_NAME_RES_AUTO);
        vc.addProperty(resAutoModel);

        final Property resPixelsModel = Property.create(PROPERTY_NAME_RES_PIXELS, Integer.class, DEFAULT_RES_PIXELS, true);
        resPixelsModel.getDescriptor().setAlias(ALIAS_NAME_RES_PIXELS);
        vc.addProperty(resPixelsModel);

        final Property resLatModel = Property.create(PROPERTY_NAME_RES_LAT, Double.class, DEFAULT_RES_LAT, true);
        resLatModel.getDescriptor().setAlias(ALIAS_NAME_RES_LAT);
        vc.addProperty(resLatModel);

        final Property resLonModel = Property.create(PROPERTY_NAME_RES_LON, Double.class, DEFAULT_RES_LON, true);
        resLonModel.getDescriptor().setAlias(ALIAS_NAME_RES_LON);
        vc.addProperty(resLonModel);

        final Property lineColorModel = Property.create(PROPERTY_NAME_LINE_COLOR, Color.class, DEFAULT_LINE_COLOR, true);
        lineColorModel.getDescriptor().setAlias(ALIAS_NAME_LINE_COLOR);
        vc.addProperty(lineColorModel);

        final Property lineTransparencyModel = Property.create(PROPERTY_NAME_LINE_TRANSPARENCY, Double.class, DEFAULT_LINE_TRANSPARENCY, true);
        lineTransparencyModel.getDescriptor().setAlias(ALIAS_NAME_LINE_TRANSPARENCY);
        vc.addProperty(lineTransparencyModel);

        final Property lineWidthModel = Property.create(PROPERTY_NAME_LINE_WIDTH, Double.class, DEFAULT_LINE_WIDTH, true);
        lineWidthModel.getDescriptor().setAlias(ALIAS_NAME_LINE_WIDTH);
        vc.addProperty(lineWidthModel);

        final Property textEnabledModel = Property.create(PROPERTY_NAME_TEXT_ENABLED, Boolean.class, DEFAULT_TEXT_ENABLED, true);
        textEnabledModel.getDescriptor().setAlias(ALIAS_NAME_TEXT_ENABLED);
        vc.addProperty(textEnabledModel);

        final Property textFontModel = Property.create(PROPERTY_NAME_TEXT_FONT, Font.class, DEFAULT_TEXT_FONT, true);
        textFontModel.getDescriptor().setAlias(ALIAS_NAME_TEXT_FONT);
        vc.addProperty(textFontModel);

        final Property textFgColorModel = Property.create(PROPERTY_NAME_TEXT_FG_COLOR, Color.class, DEFAULT_TEXT_FG_COLOR, true);
        textFgColorModel.getDescriptor().setAlias(ALIAS_NAME_TEXT_FG_COLOR);
        vc.addProperty(textFgColorModel);

        final Property textBgColorModel = Property.create(PROPERTY_NAME_TEXT_BG_COLOR, Color.class, DEFAULT_TEXT_BG_COLOR, true);
        textBgColorModel.getDescriptor().setAlias(ALIAS_NAME_TEXT_BG_COLOR);
        vc.addProperty(textBgColorModel);

        final Property textBgTransparencyModel = Property.create(PROPERTY_NAME_TEXT_BG_TRANSPARENCY, Double.class, DEFAULT_TEXT_BG_TRANSPARENCY, true);
        textBgTransparencyModel.getDescriptor().setAlias(ALIAS_NAME_TEXT_BG_TRANSPARENCY);
        vc.addProperty(textBgTransparencyModel);

        return vc;
    }
}
