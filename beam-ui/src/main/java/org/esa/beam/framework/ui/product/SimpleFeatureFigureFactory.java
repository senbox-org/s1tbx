package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.PointFigure;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.util.AwtGeomToJtsGeomConverter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;

public class SimpleFeatureFigureFactory implements FigureFactory {

    private final FeatureCollection featureCollection;
    private AwtGeomToJtsGeomConverter toJtsGeom;
    private long currentFeatureId;

    public SimpleFeatureFigureFactory(FeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
        this.toJtsGeom = new AwtGeomToJtsGeomConverter();
        this.currentFeatureId = System.nanoTime();
    }

    @Override
    public PointFigure createPointFigure(Point2D point, FigureStyle style) {
        return new SimpleFeaturePointFigure(createSimpleFeature(toJtsGeom.createPoint(point), style), style);
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

    public Figure createFigure(SimpleFeature simpleFeature, String defaultCSS) {
        Object geometry = simpleFeature.getDefaultGeometry();
        Object styleAttribute = simpleFeature.getAttribute("style");
        System.out.println("styleAttribute = [" + styleAttribute + "]");
        String css = defaultCSS;
        if (styleAttribute instanceof String && !((String)styleAttribute).isEmpty()) {
            css = (String) styleAttribute;
        }
        FigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.fromCssString(css);
        if (geometry instanceof Point) {
            return new SimpleFeaturePointFigure(simpleFeature, figureStyle);
        } else {
            return new SimpleFeatureShapeFigure(simpleFeature, figureStyle);
        }
    }

    public PointFigure createPointFigure(Point geometry, FigureStyle style) {
        return new SimpleFeaturePointFigure(createSimpleFeature(geometry, style), style);
    }

    public ShapeFigure createShapeFigure(Geometry geometry, FigureStyle style) {
        return new SimpleFeatureShapeFigure(createSimpleFeature(geometry, style), style);
    }

    public SimpleFeature createSimpleFeature(Geometry geometry, FigureStyle style) {
        SimpleFeatureType ft = (SimpleFeatureType) featureCollection.getSchema();
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);
        sfb.set("geom", geometry);
        sfb.set("style", style != null ? style.toCssString() : " ");
        return sfb.buildFeature(String.valueOf(currentFeatureId++));
    }

    public static DefaultFigureStyle createDefaultStyle() {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setStrokeColor(Color.BLACK);
        figureStyle.setFillColor(Color.WHITE);
        figureStyle.setStroke(new BasicStroke(1.0f));
        return figureStyle;
    }

    public static SimpleFeatureType createSimpleFeatureType(String typeName, Class<? extends Geometry> geometryType) {
        return createSimpleFeatureType(typeName, geometryType, DefaultGeographicCRS.WGS84);
    }

    public static SimpleFeatureType createSimpleFeatureType(String typeName,
                                                            Class<? extends Geometry> geometryType,
                                                            CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setCRS(crs);
        sftb.setName(typeName);
        sftb.add("geom", geometryType);
        sftb.add("style", String.class);
        sftb.setDefaultGeometry("geom");
        return sftb.buildFeatureType();
    }
}