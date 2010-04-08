package org.esa.beam.visat.toolviews.placemark;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.*;

public class PinPositionTest {

    private Placemark placemark;

    @Before
    public void setup() throws TransformException, FactoryException {
        final AffineTransform i2m = new AffineTransform();
        i2m.scale(2.0, 2.0);
        final GeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(0, 0, 10, 10), i2m);

        final Product product = new Product("P", "T", 10, 10);
        product.setGeoCoding(geoCoding);

        placemark = new Placemark("P1", "L", "", new PixelPos(1.0f, 1.0f), null, PinDescriptor.INSTANCE, product.getGeoCoding());
        product.getPinGroup().add(placemark);
    }

    @Test
    public void initialState() {
        final double x = placemark.getPixelPos().getX();
        final double y = placemark.getPixelPos().getY();
        assertEquals(1.0, x, 0.0);
        assertEquals(1.0, y, 0.0);

        final Point point = (Point) placemark.getFeature().getDefaultGeometry();
        assertEquals(2.0, point.getX(), 0.0);
        assertEquals(2.0, point.getY(), 0.0);

        final double lon = placemark.getGeoPos().getLon();
        final double lat = placemark.getGeoPos().getLat();
        assertEquals(2.0, lon, 0.0);
        assertEquals(2.0, lat, 0.0);
    }

    @Test
    public void movePinByGeometry() {
        placemark.getFeature().setDefaultGeometry(newPoint(4.0, 2.0));
        placemark.getProduct().getVectorDataGroup().get("pins").fireFeaturesChanged(placemark.getFeature());

        final Point point = (Point) placemark.getFeature().getDefaultGeometry();
        assertEquals(4.0, point.getX(), 0.0);
        assertEquals(2.0, point.getY(), 0.0);

        // todo: rq/?? - make asserts successful
        final double x = placemark.getPixelPos().getX();
        final double y = placemark.getPixelPos().getY();
        assertEquals(2.0, x, 0.0);
        assertEquals(1.0, y, 0.0);

        // todo: rq/?? - make asserts successful
        final double lon = placemark.getGeoPos().getLon();
        final double lat = placemark.getGeoPos().getLat();
        assertEquals(4.0, lon, 0.0);
        assertEquals(2.0, lat, 0.0);
    }

    @Test
    public void movePinByPixelPosition() {
        placemark.setPixelPos(new PixelPos(2.0f, 1.0f));

        final double x = placemark.getPixelPos().getX();
        final double y = placemark.getPixelPos().getY();
        assertEquals(2.0, x, 0.0);
        assertEquals(1.0, y, 0.0);

        // todo: rq/?? - make asserts successful
        final Point point = (Point) placemark.getFeature().getDefaultGeometry();
        assertEquals(4.0, point.getX(), 0.0);
        assertEquals(2.0, point.getY(), 0.0);

        // todo: rq/?? - make asserts successful
        final double lon = placemark.getGeoPos().getLon();
        final double lat = placemark.getGeoPos().getLat();
        assertEquals(4.0, lon, 0.0);
        assertEquals(2.0, lat, 0.0);
    }

    private Point newPoint(double x, double y) {
        return new GeometryFactory().createPoint(new Coordinate(x, y));
    }
}
