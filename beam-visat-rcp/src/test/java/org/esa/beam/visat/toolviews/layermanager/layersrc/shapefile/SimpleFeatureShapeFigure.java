

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
    private static final String GEOMETRY_TYPE_POLYGON = "Polygon";
    private static final String GEOMETRY_TYPE_MULTIPOLYGON = "MultiPolygon";

    private final SimpleFeature simpleFeature;
    private Geometry geometry;
    private String geometryType;

    public SimpleFeatureShapeFigure(SimpleFeature simpleFeature, FigureStyle style) {

        this.simpleFeature = simpleFeature;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Geometry)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        geometry = (Geometry) o;
        geometryType = geometry.getGeometryType();
        Rank rank = getRank(geometry);
        setRank(rank);
        System.out.println("___________________________________________");
        System.out.println("featureID = " + simpleFeature.getID());
        System.out.println("rank = " + rank);
        System.out.println("geometryType = " + geometry.getGeometryType());
        System.out.println("geometry.getNumGeometries() = " + geometry.getNumGeometries());
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry geom = geometry.getGeometryN(i);
            System.out.println("type = " + geom.getGeometryType());
            System.out.printf("geometry.%d = %s%n", i, geom);
        }
        System.out.println("___________________________________________");
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
        if (geometryType.equalsIgnoreCase(GEOMETRY_TYPE_POLYGON)) {
            geometry = converter.createPolygon(shape);
        } else if (geometryType.equalsIgnoreCase(GEOMETRY_TYPE_MULTIPOLYGON)) {
            geometry = converter.createMultiPolygon(shape);
        } else {
            geometry = converter.createMultiLineString(shape);
        }
        simpleFeature.setDefaultGeometry(geometry);
        fireFigureChanged();
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
