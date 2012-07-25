package org.esa.beam.pixex.output;


import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.pixex.aggregators.Aggregator;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

public class AggregatingPixExMeasurementFactory extends MeasurementFactory{

    private final RasterNamesFactory rasterNamesFactory;
    private final int windowSize;
    private final ProductRegistry productRegistry;
    private final Aggregator aggregator;

    public AggregatingPixExMeasurementFactory(final RasterNamesFactory rasterNamesFactory,
                                              final int windowSize, final ProductRegistry productRegistry,
                                              Aggregator aggregator) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.windowSize = windowSize;
        this.productRegistry = productRegistry;
        this.aggregator = aggregator;
    }

    @Override
    public Measurement[] createMeasurements(int pixelX, int pixelY, int coordinateID, String coordinateName,
                                            Product product, Raster validData) throws IOException {
        final long productId = productRegistry.getProductId(product);
        final int numPixels = windowSize * windowSize;
        final Measurement[] measurements = new Measurement[1];
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        final Number[][] values = new Number[numPixels][rasterNames.length];
        final int windowBorder = windowSize / 2;

        for (int idx = 0; idx < numPixels; idx++) {
            Arrays.fill(values[idx], Double.NaN);
            final int offsetX = idx % windowSize;
            final int offsetY = idx / windowSize;
            int x = pixelX - windowBorder + offsetX;
            int y = pixelY - windowBorder + offsetY;
            Number[] bandValues = new Number[rasterNames.length];
            setBandValues(product, rasterNames, x, y, bandValues);
            values[idx] = bandValues;
        }
        Number[] aggregatedMeasurementValues = aggregator.aggregateMeasuresForBands(values, numPixels,
                                                                                      rasterNames.length,
                                                                                      getRasterDatatypes(product));
        measurements[0] = createMeasurement(product, productId, coordinateID, coordinateName, aggregatedMeasurementValues,
                                            validData, pixelX, pixelY);
        return measurements;
    }

    private int[] getRasterDatatypes(Product product) {
        final int[] result = new int[product.getNumBands()];
        Band[] bands = product.getBands();
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            result[i] = band.getDataType();
        }
        return result;
    }

}
