package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.measurement.Measurement;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PixExMeasurementFactoryTest {

    private Product product;
    private RasterNamesFactory rasterNamesFactory;
    private Band band1;
    private Band band2;
    private Band band3;
    private ProductRegistry productRegistry;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 10, 10);

        band1 = product.addBand("val1", ProductData.TYPE_INT16);
        fillValues(band1, 10);

        band2 = product.addBand("val2", ProductData.TYPE_FLOAT32);
        fillValues(band2, 20);

        band3 = product.addBand("val3", ProductData.TYPE_UINT32);
        fillValues(band3, 231450709);

        rasterNamesFactory = newRasterNamesFactory();
        productRegistry = newProductRegistry();
    }

    @Test
    public void testMeasurementCreation() throws IOException {
        // preparation
        final int windowSize = 3;
        final PixExMeasurementFactory factory = new PixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                            productRegistry);

        // execution
        final int pixelX = 3;
        final int pixelY = 4;
        final int coordinateID = 2345;
        final String cordName = "CordName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, cordName, product,
                                                                      null);

        // verifying
        assertThat(measurements.length, equalTo(9));

        final Measurement[] expectedMeasurements = new Measurement[9];
        for (int i = 0; i < expectedMeasurements.length; i++) {
            expectedMeasurements[i] = createExpectedMeasurement(windowSize, pixelX, pixelY, coordinateID, cordName, i);
        }
        // todo: this fails - check this!!
        assertThat(measurements, equalTo(expectedMeasurements));
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement createExpectedMeasurement(int windowSize, int pixelX, int pixelY, int coordinateID,
                                                  String cordName, int i) throws IOException {
        final int windowOffset = windowSize / 2;

        final int xOffset = i % windowSize;
        final int yOffset = i / windowSize;

        final int upperLeftX = pixelX - windowOffset;
        final int upperLeftY = pixelY - windowOffset;

        final int pixX = upperLeftX + xOffset;
        final int pixY = upperLeftY + yOffset;

        final Integer intValue = band1.getPixelInt(pixX, pixY);
        final Double floatValue = band2.getPixelDouble(pixX, pixY);
        final Long uintValue = band3.getPixelInt(pixX, pixY) & 0xFFFFFFFFL;
        final Number[] values = {intValue, floatValue, uintValue};

        final long productId = productRegistry.getProductId(product);

        return new Measurement(coordinateID, cordName, productId, 0.5f + pixX, 0.5f + pixY, null, new GeoPos(), values,
                               true);
    }

    private ProductRegistry newProductRegistry() {
        return new ProductRegistry() {
            @Override
            public long getProductId(Product product) {
                return 1234;
            }

            @Override
            public void close() {
            }
        };
    }

    private RasterNamesFactory newRasterNamesFactory() {
        return new RasterNamesFactory() {
            @Override
            public String[] getRasterNames(Product product) {
                return product.getBandNames();
            }

            @Override
            public String[] getUniqueRasterNames(Product product) {
                return getRasterNames(product);
            }
        };
    }

    private void fillValues(Band band, int offset) {
        final ProductData data = band.createCompatibleRasterData();
        final int numElems = data.getNumElems();
        for (int i = 0; i < numElems; i++) {
            data.setElemDoubleAt(i, 0.5 + i + offset);
        }
        band.setData(data);
    }
}
