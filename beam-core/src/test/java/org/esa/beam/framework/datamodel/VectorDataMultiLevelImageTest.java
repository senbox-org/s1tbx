package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class VectorDataMultiLevelImageTest {

    private Product product;
    private VectorDataNode pyramids;
    private VectorDataMultiLevelImage image;

    @Before
    public void setup() {
        product = new Product("P", "T", 11, 11);
        pyramids = new VectorDataNode("pyramids", createPyramidFeatureType());
        product.getVectorDataGroup().add(pyramids);

        image = new VectorDataMultiLevelImage(VectorDataMultiLevelImage.createMaskMultiLevelSource(pyramids), pyramids);
    }

    @Test
    public void imageIsUpdated() {
        assertTrue(0 == image.getImage(0).getData().getSample(0, 0, 0));
        assertTrue(0 == image.getImage(0).getData().getSample(5, 5, 0));
        pyramids.getFeatureCollection().add(
                createPyramidFeature("Cheops", new Rectangle2D.Double(2.0, 2.0, 7.0, 7.0)));
        assertTrue(0 == image.getImage(0).getData().getSample(0, 0, 0));
        assertTrue(0 != image.getImage(0).getData().getSample(5, 5, 0));
    }

    @Test
    public void listenerIsAdded() {
        assertTrue(Arrays.asList(product.getProductNodeListeners()).contains(image));
    }

    @Test
    public void listenerIsRemoved() {
        image.dispose();
        assertFalse(Arrays.asList(product.getProductNodeListeners()).contains(image));
    }

    @Test
    public void vectorDataIsSet() {
        assertSame(pyramids, image.getVectorData());
    }

    @Test
    public void vectorDataIsClearedWhenImagesIsDisposed() {
        image.dispose();
        assertNull(image.getVectorData());
    }

    private static SimpleFeature createPyramidFeature(String name, Rectangle2D rectangle) {
        final SimpleFeatureType type = createPyramidFeatureType();
        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        final GeometryFactory geometryFactory = new GeometryFactory();
        final double x = rectangle.getX();
        final double y = rectangle.getY();
        final double w = rectangle.getWidth();
        final double h = rectangle.getHeight();
        final LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + w, y),
                new Coordinate(x + w, y + h),
                new Coordinate(x, y + h),
                new Coordinate(x, y),
        });
        featureBuilder.add(geometryFactory.createPolygon(linearRing, null));
        return featureBuilder.buildFeature(name);
    }

    private static SimpleFeatureType createPyramidFeatureType() {
        final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(crs);
        builder.setName("PyramidType");
        builder.add("geometry", Polygon.class, crs);
        builder.setDefaultGeometry("geometry");

        return builder.buildFeatureType();
    }
}
