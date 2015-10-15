package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.MeasurementFactory;
import org.esa.snap.pixex.aggregators.AggregatorStrategy;
import org.esa.snap.pixex.aggregators.MaxAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MeanAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MedianAggregatorStrategy;
import org.esa.snap.pixex.aggregators.MinAggregatorStrategy;
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
                                                                        17.0F, 3.5707142F, 9, // mean, sigma, num_pixels
                                                                        25.5F, 3.5707142F, 9 // mean, sigma, num_pixels
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
                                                                    coordsName, 12f, 9, 20.5f, 9);
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
                                                                    coordsName, 22f, 9, 30.5f, 9);
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
                                                                    coordsName, 17f, 9, 25.5f, 9);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testMeanWithFillValues_float() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MeanAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, 5,
                                                                                  productRegistry, aggregatorStrategy);
        Product fillValueContainingProduct = new Product("p0", "t0", 5, 5);
        Band band = fillValueContainingProduct.addBand("b0", ProductData.TYPE_FLOAT32);
        band.setNoDataValue(-1.0);
        band.setData(new ProductData.Float(new float[]{
                1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                -1.0F, -1.0F, -1.0F, -1.0F, -1.0F
        }));

        // execution
        final int pixelX = 2;
        final int pixelY = 2;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, fillValueContainingProduct,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Number[] expectedValues = {
                1.0F, 0.0F, 20 // mean, sigma, num_pixels
        };
        final Measurement expectedMeasurement = new Measurement(coordinateID, coordsName, 1234L, 2.5F, 2.5F, null,
                                                                new GeoPos(),
                                                                expectedValues, true);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testMeanWithFillValues_int() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MeanAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, 5,
                                                                                  productRegistry, aggregatorStrategy);
        Product fillValueContainingProduct = new Product("p0", "t0", 5, 5);
        Band band = fillValueContainingProduct.addBand("b0", ProductData.TYPE_INT16);
        band.setNoDataValue(16);
        band.setData(new ProductData.Short(new short[]{
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                16, 16, 16, 16, 16
        }));

        // execution
        final int pixelX = 2;
        final int pixelY = 2;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, fillValueContainingProduct,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Number[] expectedValues = {
                1.0F, 0.0F, 20 // mean, sigma, num_pixels
        };
        final Measurement expectedMeasurement = new Measurement(coordinateID, coordsName, 1234L, 2.5F, 2.5F, null,
                                                                new GeoPos(),
                                                                expectedValues, true);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    @Test
    public void testMeanWithFillValues_uint32() throws Exception {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new MeanAggregatorStrategy();
        final MeasurementFactory factory = new AggregatingPixExMeasurementFactory(rasterNamesFactory, 5,
                                                                                  productRegistry, aggregatorStrategy);
        Product fillValueContainingProduct = new Product("p0", "t0", 5, 5);
        Band band = fillValueContainingProduct.addBand("b0", ProductData.TYPE_UINT32);
        band.setNoDataValue(16);
        band.setData(new ProductData.UInt(new int[]{
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                16, 16, 16, 16, 16
        }));

        // execution
        final int pixelX = 2;
        final int pixelY = 2;
        final int coordinateID = 2345;
        final String coordsName = "coordsName";
        final Measurement[] measurements = factory.createMeasurements(pixelX, pixelY, coordinateID, coordsName, fillValueContainingProduct,
                                                                      null);

        // verifying
        assertEquals(1, measurements.length);

        Number[] expectedValues = {
                1.0F, 0.0F, 20 // mean, sigma, num_pixels
        };
        final Measurement expectedMeasurement = new Measurement(coordinateID, coordsName, 1234L, 2.5F, 2.5F, null,
                                                                new GeoPos(),
                                                                expectedValues, true);
        assertEquals(expectedMeasurement, measurements[0]);
    }

    /*/////////////////////////////
    ////  Test Helper Methods  ////
    /////////////////////////////*/

    private Measurement createExpectedMeasurement(int pixelX, int pixelY, int coordinateID,
                                                  String coordsName, Number... expectedValues) throws IOException {
        final Number[] values = new Number[expectedValues.length];
        System.arraycopy(expectedValues, 0, values, 0, values.length);

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
