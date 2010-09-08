package org.esa.beam.pet;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * User: Marco
 * Date: 04.09.2010
 */
public class PetOpTest {

    @Test
    public void testReadMeasurement() throws TransformException, FactoryException, IOException {
        PetOp op = new PetOp();
        String[] bandNames = {"band_1", "band_2", "band_3"};
        op.rasterNames = bandNames;
        op.squareSize = 3;
        Product product = createTestProduct(bandNames);
        Map<String, List<Measurement>> measurements = new HashMap<String, List<Measurement>>();
        GeoPos geoPos = new GeoPos(20, 10);
        op.readMeasurement(product, new Coordinate(1, geoPos), measurements);
        geoPos = new GeoPos(21, 9);

        List<Measurement> measurementList = measurements.get(product.getProductType());
        assertNotNull(measurementList);
        assertTrue(measurementList.size() > 0);

        for (int i = 0; i < measurementList.size(); i++) {
            assertEquals(3 * 3, measurementList.size());
            Measurement measurement = measurementList.get(i);
            assertEquals(1, measurement.getCoordinateID());
            assertEquals(geoPos.lat - i / 3, measurement.getLat(), 1.0e-4);
            assertEquals(geoPos.lon + i % 3, measurement.getLon(), 1.0e-4);
            assertEquals(" ", measurement.getCoordinateName());
            assertNull(measurement.getStartTime());
            double[] values = measurement.getValues();
            assertEquals(bandNames.length, values.length);
            assertEquals(0, values[0], 1.0e-4);
            assertEquals(1, values[1], 1.0e-4);
            assertEquals(2, values[2], 1.0e-4);
        }
    }

    private Product createTestProduct(String[] bandNames) throws FactoryException, TransformException {
        Rectangle bounds = new Rectangle(360, 180);
        Product product = new Product("test", "TEST", bounds.width, bounds.height);
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 90;
        final int easting = -180;
        i2mTransform.translate(easting, northing);
        final double scaleX = 360 / bounds.width;
        final double scaleY = 180 / bounds.height;
        i2mTransform.scale(scaleX, -scaleY);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, bounds, i2mTransform);
        product.setGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setDataElems(generateData(bounds, i));
        }
        return product;
    }

    private float[] generateData(Rectangle bounds, int val) {
        float[] floats = new float[bounds.width * bounds.height];
        Arrays.fill(floats, val);
        return floats;
    }

}
