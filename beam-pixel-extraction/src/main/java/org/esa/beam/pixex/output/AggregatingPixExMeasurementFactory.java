package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.pixex.aggregators.AggregatorStrategy;
import org.esa.beam.pixex.calvalus.ma.DefaultRecord;
import org.esa.beam.pixex.calvalus.ma.Record;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

public class AggregatingPixExMeasurementFactory extends AbstractMeasurementFactory {

    private final RasterNamesFactory rasterNamesFactory;
    private final int windowSize;
    private final ProductRegistry productRegistry;
    private final AggregatorStrategy aggregatorStrategy;

    public AggregatingPixExMeasurementFactory(final RasterNamesFactory rasterNamesFactory,
                                              final int windowSize, final ProductRegistry productRegistry,
                                              AggregatorStrategy aggregatorStrategy) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.windowSize = windowSize;
        this.productRegistry = productRegistry;
        this.aggregatorStrategy = aggregatorStrategy;
    }

    @Override
    public Measurement[] createMeasurements(int pixelX, int pixelY, int coordinateID, String coordinateName,
                                            Product product, Raster validData) throws IOException {
        final long productId = productRegistry.getProductId(product);
        final int numPixels = windowSize * windowSize;
        final Measurement[] measurements = new Measurement[1];
        final String[] rasterNames = rasterNamesFactory.getUniqueRasterNames(product);
        final Float[][] values = new Float[rasterNames.length][numPixels];

        for (int i = 0; i < rasterNames.length; i++) {
            String rasterName = rasterNames[i];
            Float[] bandValues = new Float[numPixels];
            final Band band = product.getBand(rasterName);
            setBandValues(product, band, bandValues, windowSize, pixelX, pixelY);
            values[i] = bandValues;
        }

        Record record = new DefaultRecord(values);
        final float[] numbers = new float[rasterNames.length * aggregatorStrategy.getValueCount()];
        for (int i = 0; i < rasterNames.length; i++) {
            float[] valuesForBand = aggregatorStrategy.getValues(record, i);
            for (int j = 0; j < aggregatorStrategy.getValueCount(); j++) {
                float v = valuesForBand[j];
                numbers[i * aggregatorStrategy.getValueCount() + j] = v;
            }
        }

        measurements[0] = createMeasurement(product, productId, coordinateID, coordinateName,
                                            createFloatArray(numbers), validData,
                                            pixelX, pixelY);
        return measurements;
    }

    private Float[] createFloatArray(float[] values) {
        final Float[] result = new Float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    private static void setBandValues(Product product, RasterDataNode raster, Float[] bandValues,
                                      int windowSize, int pixelX, int pixelY) {

        final int windowBorder = windowSize / 2;

        if (raster == null) {
            Arrays.fill(bandValues, Float.NaN);
            return;
        }

        int pixelIndex = 0;
        for (int x = pixelX - windowBorder; x <= pixelX + windowBorder; x++) {
            for (int y = pixelY - windowBorder; y <= pixelY + windowBorder; y++) {
                if (product.containsPixel(x, y)) {
                    if (!raster.isPixelValid(x, y)) {
                        bandValues[pixelIndex] = Float.NaN;
                    } else if (raster.isFloatingPointType()) {
                        bandValues[pixelIndex] = raster.getSampleFloat(x, y);
                    } else {
                        int temp = raster.getSampleInt(x, y);
                        if (raster instanceof Mask) {
                            bandValues[pixelIndex] = (float) (temp == 0 ? 0 : 1); // normalize to 0 for false and 1 for true
                        } else {
                            if (raster.getDataType() == ProductData.TYPE_UINT32) {
                                bandValues[pixelIndex] = (float) (temp & 0xffffL);
                            } else {
                                bandValues[pixelIndex] = (float) temp;
                            }
                        }
                    }
                }
                pixelIndex++;
            }
        }
    }

    @Override
    public void close() {
        productRegistry.close();
    }
}
