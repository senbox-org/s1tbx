package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.pixex.aggregators.MaxAggregator;
import org.esa.beam.pixex.aggregators.MeanAggregator;
import org.esa.beam.pixex.aggregators.Aggregator;
import org.esa.beam.pixex.aggregators.MedianAggregator;
import org.esa.beam.pixex.aggregators.MinAggregator;
import org.junit.Before;
import org.junit.Ignore;
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
        final Aggregator aggregator = new MeanAggregator();
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
                                                                          coordsName, 144 / 9, 229.5f / 9f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Ignore
    @Test
    public void testCreateMeasurementsWithMinMeasurementAggregator() throws Exception {
        // preparation
        final Aggregator aggregator = new MinAggregator();
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
                                                                    coordsName, 11, 20.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Ignore
    @Test
    public void testCreateMeasurementsWithMaxMeasurementAggregator() throws Exception {
        // preparation
        final Aggregator aggregator = new MaxAggregator();
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
                                                                    coordsName, 21, 30.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Ignore
    @Test
    public void testCreateMeasurementsWithMedianMeasurementAggregator() throws Exception {
        // preparation
        final Aggregator aggregator = new MedianAggregator();
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
                                                                    coordsName, 16, 25.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement createExpectedMeasurement(int pixelX, int pixelY, int coordinateID,
                                                  String coordsName, int firstExpectedValue,
                                                  float secondExpectedValue) throws
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
            data.setElemFloatAt(i, (float) (0.5 + i + offset));
        }
        band.setData(data);
    }

}
