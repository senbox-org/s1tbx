

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.figure.AbstractShapeFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.LiteShape2;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.Shape;


public class SimpleFeatureShapeFigure extends AbstractShapeFigure implements SimpleFeatureFigure {

    private final SimpleFeature simpleFeature;
    private Geometry geometry;

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, FigureStyle style) {

        this.simpleFeature = simpleFeature;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Geometry)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        geometry = (Geometry) o;
        Rank rank = getRank(geometry);
        System.out.println("rank = " + rank);
        setRank(rank);
        setNormalStyle(style);
    }

    @Override
    public Shape getShape() {
        try {
            return new LiteShape2(geometry, null, null, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("simpleFeature", e);
        }
    }

    @Override
    public void setShape(Shape shape) {
        AwtGeomToJtsGeomConverter converter = new AwtGeomToJtsGeomConverter();
        if (getRank() == Rank.POLYGONAL) {
            geometry = converter.createPolygon(shape);
        } else {
            geometry = converter.createMultiLineString(shape);
        }
        simpleFeature.setDefaultGeometry(geometry);
        fireFigureChanged();
        System.out.println("geometry: " + geometry);
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    public static Rank getRank(Geometry geometry) {
        if (geometry instanceof Point) {
            return Rank.PUNCTUAL;
        }
        if (geometry instanceof MultiPoint) {
            return Rank.PUNCTUAL;
        }
        if (geometry instanceof LineString) {
            return Rank.LINEAL;
        }
        if (geometry instanceof MultiLineString) {
            return Rank.LINEAL;
        }
        if (geometry instanceof GeometryCollection) {
            return Rank.COLLECTION;
        }
        return Rank.POLYGONAL;
    }
}
