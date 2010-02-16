package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.Figure;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import junit.framework.TestCase;
import static org.esa.beam.framework.datamodel.PlainFeatureFactory.createPlainFeature;
import static org.esa.beam.framework.datamodel.PlainFeatureFactory.createPlainFeatureType;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class SimpleFeatureShapeFigureTest extends TestCase {
    private final GeometryFactory gf = new GeometryFactory();

    public void testSpecificGeometryType() {
        SimpleFeatureType sft = createPlainFeatureType("Polygon", Polygon.class, DefaultGeographicCRS.WGS84);

        Polygon polygon = createPolygon();
        SimpleFeature simpleFeature = createPlainFeature(sft, "_1", polygon, "");

        SimpleFeatureShapeFigure shapeFigure = new SimpleFeatureShapeFigure(simpleFeature, null);
        assertSame(polygon, shapeFigure.getGeometry());
        assertNotNull(shapeFigure.getShape());
        assertEquals(Figure.Rank.AREA, shapeFigure.getRank());
    }

    public void testMixedGeometries() {
        SimpleFeatureType sft = createPlainFeatureType("Geometry", Geometry.class, DefaultGeographicCRS.WGS84);

        Geometry geometry;
        SimpleFeature feature;
        SimpleFeatureShapeFigure figure;

        geometry = createPolygon();
        feature = createPlainFeature(sft, "_1", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, null);
        assertSame(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.AREA, figure.getRank());

        geometry = createLinearRing();
        feature = createPlainFeature(sft, "_2", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, null);
        assertSame(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.LINE, figure.getRank());

        geometry = createLineString();
        feature = createPlainFeature(sft, "_3", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, null);
        assertSame(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.LINE, figure.getRank());

        geometry = createPoint();
        feature = createPlainFeature(sft, "_4", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, null);
        assertSame(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.POINT, figure.getRank());

        geometry = createGeometryCollection();
        feature = createPlainFeature(sft, "_5", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, null);
        assertSame(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.NOT_SPECIFIED, figure.getRank());
    }

    public void testRank() {
        assertEquals(Figure.Rank.POINT, SimpleFeatureShapeFigure.getRank(createPoint()));
        assertEquals(Figure.Rank.POINT, SimpleFeatureShapeFigure.getRank(createMultiPoint()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createLineString()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createLinearRing()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createMultiLineString()));
        assertEquals(Figure.Rank.AREA, SimpleFeatureShapeFigure.getRank(createPolygon()));
        assertEquals(Figure.Rank.AREA, SimpleFeatureShapeFigure.getRank(createMultiPolygon()));
        assertEquals(Figure.Rank.NOT_SPECIFIED, SimpleFeatureShapeFigure.getRank(createGeometryCollection()));
    }

    private Point createPoint() {
        return gf.createPoint(new Coordinate(0, 0));
    }

    private LineString createLineString() {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
        });
    }

    private LinearRing createLinearRing() {
        return gf.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
                new Coordinate(0, 0),
        });
    }

    private Polygon createPolygon() {
        return gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0, 1),
                new Coordinate(0, 0),
        }), null);
    }

    private MultiPoint createMultiPoint() {
        return gf.createMultiPoint(new Point[0]);
    }

    private MultiPolygon createMultiPolygon() {
        return gf.createMultiPolygon(new Polygon[0]);
    }

    private MultiLineString createMultiLineString() {
        return gf.createMultiLineString(new LineString[0]);
    }

    private GeometryCollection createGeometryCollection() {
        return gf.createGeometryCollection(new Geometry[0]);
    }
}
