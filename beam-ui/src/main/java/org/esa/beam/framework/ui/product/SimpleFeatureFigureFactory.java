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

package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.PointFigure;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.util.AwtGeomToJtsGeomConverter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.awt.geom.Point2D;

public class SimpleFeatureFigureFactory implements FigureFactory {

    private final SimpleFeatureType simpleFeatureType;
    private final AwtGeomToJtsGeomConverter toJtsGeom;
    private long currentFeatureId;

    public SimpleFeatureFigureFactory(SimpleFeatureType simpleFeatureType) {
        this.simpleFeatureType = simpleFeatureType;
        this.toJtsGeom = new AwtGeomToJtsGeomConverter();
        this.currentFeatureId = System.nanoTime();
    }


    @Override
    public PointFigure createPointFigure(Point2D point, FigureStyle style) {
        final Point point1 = toJtsGeom.createPoint(point);
        return createPointFigure(point1, style);
    }

    @Override
    public ShapeFigure createLineFigure(Shape shape, FigureStyle style) {
        MultiLineString multiLineString = toJtsGeom.createMultiLineString(shape);
        if (multiLineString.getNumGeometries() == 1) {
            return createShapeFigure(multiLineString.getGeometryN(0), style);
        } else {
            return createShapeFigure(multiLineString, style);
        }
    }

    @Override
    public ShapeFigure createPolygonFigure(Shape shape, FigureStyle style) {
        Polygon polygon = toJtsGeom.createPolygon(shape);
        return createShapeFigure(polygon, style);
    }

    public PointFigure createPointFigure(Point geometry, FigureStyle style) {
        return new SimpleFeaturePointFigure(createSimpleFeature(geometry), style);
    }

    public SimpleFeatureFigure createSimpleFeatureFigure(SimpleFeature simpleFeature, String defaultStyleCss) {
        final String css = getStyleCss(simpleFeature, defaultStyleCss);
        final FigureStyle normalStyle = DefaultFigureStyle.createFromCss(css);
        final FigureStyle selectedStyle = deriveSelectedStyle(normalStyle);
        final Object geometry = simpleFeature.getDefaultGeometry();
        if (geometry instanceof Point) {
            return new SimpleFeaturePointFigure(simpleFeature, normalStyle, selectedStyle);
        } else {
            return new SimpleFeatureShapeFigure(simpleFeature, normalStyle, selectedStyle);
        }
    }

    private String getStyleCss(SimpleFeature simpleFeature, String defaultStyleCss) {
        Object styleAttribute = simpleFeature.getAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS);
        if (styleAttribute instanceof String) {
            String css = (String) styleAttribute;
            if (!css.trim().isEmpty()) {
                return css;
            }
        }
        return defaultStyleCss;
    }

    public ShapeFigure createShapeFigure(Geometry geometry, FigureStyle style) {
        return new SimpleFeatureShapeFigure(createSimpleFeature(geometry), style, deriveSelectedStyle(style));
    }

    public SimpleFeature createSimpleFeature(Geometry geometry) {
        return PlainFeatureFactory.createPlainFeature(simpleFeatureType,
                                                      "ID" + Long.toHexString(currentFeatureId++),
                                                      geometry,
                                                      null);
    }

    public FigureStyle deriveSelectedStyle(FigureStyle style) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setFillColor(style.getFillColor());
        figureStyle.setFillOpacity(style.getFillOpacity());
        figureStyle.setStrokeColor(Color.YELLOW);
        figureStyle.setStrokeOpacity(0.75);
        figureStyle.setStrokeWidth(style.getStrokeWidth() + 1.0);
        figureStyle.setSymbolName(style.getSymbolName());
        figureStyle.setSymbolImagePath(style.getSymbolImagePath());
        figureStyle.setSymbolRefX(style.getSymbolRefX());
        figureStyle.setSymbolRefY(style.getSymbolRefY());
        return figureStyle;
    }

}
