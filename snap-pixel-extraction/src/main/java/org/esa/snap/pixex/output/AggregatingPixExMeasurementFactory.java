package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.pixex.aggregators.AggregatorStrategy;
import org.esa.snap.pixex.calvalus.ma.DefaultRecord;
import org.esa.snap.pixex.calvalus.ma.Record;

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
            setBandValues(product, band, bandValues, windowSize, pixelX, pixelY, validData);
            values[i] = bandValues;
        }

        Record record = new DefaultRecord(values);
        final Number[] numbers = new Number[rasterNames.length * aggregatorStrategy.getValueCount()];
        for (int i = 0; i < rasterNames.length; i++) {
            Number[] valuesForBand = aggregatorStrategy.getValues(record, i);
            for (int j = 0; j < aggregatorStrategy.getValueCount(); j++) {
                Number v = valuesForBand[j];
                numbers[i * aggregatorStrategy.getValueCount() + j] = v;
            }
        }

        measurements[0] = createMeasurement(product, productId, coordinateID, coordinateName,
                                            numbers, true, pixelX, pixelY);
        return measurements;
    }

    @Override
    public void close() {
        productRegistry.close();
    }

    private static void setBandValues(Product product, RasterDataNode raster, Float[] bandValues,
                                      int windowSize, int pixelX, int pixelY, Raster validData) {

        final int windowBorder = windowSize / 2;

        if (raster == null) {
            Arrays.fill(bandValues, Float.NaN);
            return;
        }

        int pixelIndex = 0;
        for (int x = pixelX - windowBorder; x <= pixelX + windowBorder; x++) {
            for (int y = pixelY - windowBorder; y <= pixelY + windowBorder; y++) {
                if (product.containsPixel(x, y)) {
                    if (!raster.isPixelValid(x, y) || (validData != null && validData.getSample(x, y, 0) == 0)) {
                        bandValues[pixelIndex] = Float.NaN;
                    } else if (raster.isFloatingPointType()) {
                        float sampleFloat = raster.getSampleFloat(x, y);
                        bandValues[pixelIndex] = isNoDataValue(raster, sampleFloat) ? Float.NaN : sampleFloat;
                    } else {
                        int temp = raster.getSampleInt(x, y);
                        if (raster instanceof Mask) {
                            bandValues[pixelIndex] = (float) (temp == 0 ? 0 : 1); // normalize to 0 for false and 1 for true
                        } else {
                            if (raster.getDataType() == ProductData.TYPE_UINT32) {
                                if (isNoDataValue(raster, temp)) {
                                    bandValues[pixelIndex] = Float.NaN;
                                } else {
                                    bandValues[pixelIndex] = (float) (temp & 0xffffL);
                                }
                            } else {
                                if (isNoDataValue(raster, temp)) {
                                    bandValues[pixelIndex] = Float.NaN;
                                } else {
                                    bandValues[pixelIndex] = (float) temp;
                                }
                            }
                        }
                    }
                }
                pixelIndex++;
            }
        }
    }

    private static boolean isNoDataValue(RasterDataNode raster, int sample) {
        return !raster.isNoDataValueUsed() && sample == raster.getNoDataValue();
    }

    private static boolean isNoDataValue(RasterDataNode raster, float sample) {
        return !raster.isNoDataValueUsed() && sample == raster.getNoDataValue();
    }

}
