package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.measurement.Measurement;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AggregatingPixExMeasurementFactoryTest {

    private Product product;
    private RasterNamesFactory rasterNamesFactory;
    private Band band1;
    private Band band2;
    private ProductRegistry productRegistry;
    private int windowSize;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 4, 4);

        band1 = product.addBand("val1", ProductData.TYPE_INT16);
        fillValues(band1, 10);

        band2 = product.addBand("val2", ProductData.TYPE_FLOAT32);
        fillValues(band2, 20);

        rasterNamesFactory = createNewRasterNamesFactory();
        productRegistry = createNewProductRegistry();

        windowSize = 3;
    }

    @Test
    public void testCreateMeasurementsWithMeanMeasurementAggregator() throws Exception {
        // preparation
        final MeasurementAggregator aggregator = new MeanMeasurementAggregator();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregator);

        // execution
        final int pixelX = 1;
        final int pixelY = 1;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, product,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Measurement expectedMeasurement = createExpectedMeasurement(pixelX, pixelY, coordinateID,
                                                                          coordsName, 144 / 9, 229.5 / 9.0);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMinMeasurementAggregator() throws Exception {
        // preparation
        final MeasurementAggregator aggregator = new MinMeasurementAggregator();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregator);

        // execution
        final int pixelX = 1;
        final int pixelY = 1;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, product,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Measurement expectedMeasurement = createExpectedMeasurement(pixelX, pixelY, coordinateID,
                                                                    coordsName, 11, 20.5);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMaxMeasurementAggregator() throws Exception {
        // preparation
        final MeasurementAggregator aggregator = new MaxMeasurementAggregator();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregator);

        // execution
        final int pixelX = 1;
        final int pixelY = 1;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, product,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Measurement expectedMeasurement = createExpectedMeasurement(pixelX, pixelY, coordinateID,
                                                                    coordsName, 21, 30.5);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMedianMeasurementAggregator() throws Exception {
        // preparation
        final MeasurementAggregator aggregator = new MedianMeasurementAggregator();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregator);

        // execution
        final int pixelX = 1;
        final int pixelY = 1;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, product,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Measurement expectedMeasurement = createExpectedMeasurement(pixelX, pixelY, coordinateID,
                                                                    coordsName, 16, 25.5);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement createExpectedMeasurement(int pixelX, int pixelY, int coordinateID,
                                                  String coordsName, int firstExpectedValue,
                                                  double secondExpectedValue) throws
                                                                              IOException {
        final Number[] values = {firstExpectedValue, secondExpectedValue};

        final long productId = productRegistry.getProductId(product);

        return new Measurement(coordinateID, coordsName, productId, pixelX+0.5f, pixelY+0.5f, null, new GeoPos(),
                               values, true);
    }

    private ProductRegistry createNewProductRegistry() {
        return new ProductRegistry() {
            @Override
            public long getProductId(Product product) {
                return 1234;
            }
        };
    }

    private RasterNamesFactory createNewRasterNamesFactory() {
        return new RasterNamesFactory() {
            @Override
            public String[] getRasterNames(Product product) {
                return product.getBandNames();
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
