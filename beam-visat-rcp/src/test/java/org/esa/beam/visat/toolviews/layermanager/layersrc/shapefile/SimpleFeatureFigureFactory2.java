package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;

class SimpleFeatureFigureFactory2 implements FigureFactory {

    private final FeatureCollection featureCollection;
    private AwtGeomToJtsGeomConverter toJtsGeom;
    private long currentFeatureId;

    SimpleFeatureFigureFactory2(FeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
        this.toJtsGeom = new AwtGeomToJtsGeomConverter();
        this.currentFeatureId = System.nanoTime();
    }

    @Override
    public Figure createPunctualFigure(Point2D geometry, FigureStyle style) {
        return null;
    }

    @Override
    public ShapeFigure createLinealFigure(Shape geometry, FigureStyle style) {
        MultiLineString multiLineString = toJtsGeom.createMultiLineString(geometry);
        if (multiLineString.getNumGeometries() == 1) {
            return createShapeFigure(multiLineString.getGeometryN(0), style);
        } else {
            return createShapeFigure(multiLineString, style);
        }
    }

    @Override
    public ShapeFigure createPolygonalFigure(Shape geometry, FigureStyle style) {
        Polygon polygon = toJtsGeom.createPolygon(geometry);
        return createShapeFigure(polygon, style);
    }

    @Override
    public Figure createCollectionFigure(Figure... figures) {
        return null;
    }

    public ShapeFigure createShapeFigure(Geometry geometry1, FigureStyle style) {
        SimpleFeatureType ft = (SimpleFeatureType) featureCollection.getSchema();
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);
        sfb.set("geometry", geometry1);
        return new SimpleFeatureFigure2(sfb.buildFeature(String.valueOf(currentFeatureId++)), style);
    }

    public static DefaultFigureStyle createDefaultStyle() {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setStrokePaint(Color.BLACK);
        figureStyle.setFillPaint(Color.WHITE);
        return figureStyle;
    }
}