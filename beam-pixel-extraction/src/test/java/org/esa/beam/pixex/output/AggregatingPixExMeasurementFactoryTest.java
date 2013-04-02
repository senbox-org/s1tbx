package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.MeasurementFactory;
import org.esa.beam.pixex.aggregators.AggregatorStrategy;
import org.esa.beam.pixex.aggregators.MaxAggregatorStrategy;
import org.esa.beam.pixex.aggregators.MeanAggregatorStrategy;
import org.esa.beam.pixex.aggregators.MedianAggregatorStrategy;
import org.esa.beam.pixex.aggregators.MinAggregatorStrategy;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class AggregatingPixExMeasurementFactoryTest {

    private Product product;
    private RasterNamesFactory rasterNamesFactory;
    private ProductRegistry productRegistry;
    private int windowSize;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 4, 4);

        Band band1 = product.addBand("val1", ProductData.TYPE_INT32);
        fillValues(band1, 11);

        Band band2 = product.addBand("val2", ProductData.TYPE_FLOAT32);
        fillValues(band2, 20);

        rasterNamesFactory = createNewRasterNamesFactory();
        productRegistry = createNewProductRegistry();

        windowSize = 3;
    }

    @Test
    public void testCreateMeasurementsWithMeanMeasurementAggregator() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MeanAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregatorStrategy);

        // execution
        final int pixelX = 1;
        final int pixelY = 1;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, product,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        final Measurement expectedMeasurement = new Measurement(coordinateID, coordsName, 1234L, 1.5F, 1.5F, null,
                                                                new GeoPos(),
                                                                new Number[]{
                                                                        17.0F, 3.5707142F, 25.5F, 3.5707142F
                                                                }, true);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMinMeasurementAggregator() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MinAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregatorStrategy);

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
                                                                    coordsName, 12, 20.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMaxMeasurementAggregator() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MaxAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregatorStrategy);

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
                                                                    coordsName, 22, 30.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testCreateMeasurementsWithMedianMeasurementAggregator() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MedianAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, windowSize,
                                                                                  productRegistry, aggregatorStrategy);

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
                                                                    coordsName, 17, 25.5f);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement createExpectedMeasurement(int pixelX, int pixelY, int coordinateID,
                                                  String coordsName, float firstExpectedValue,
                                                  float secondExpectedValue) throws
                                                                             IOException {
        final Number[] values = {firstExpectedValue, secondExpectedValue};

        final long productId = productRegistry.getProductId(product);

        return new Measurement(coordinateID, coordsName, productId, pixelX + 0.5f, pixelY + 0.5f, null, new GeoPos(),
                               values, true);
    }

    private ProductRegistry createNewProductRegistry() {
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

    private RasterNamesFactory createNewRasterNamesFactory() {
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
            data.setElemFloatAt(i, (float) (0.5 + i + offset));
        }
        band.setData(data);
    }

}
