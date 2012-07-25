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
import org.esa.beam.pixex.calvalus.ma.RecordAggregator;
import org.esa.beam.pixex.calvalus.ma.RecordTransformer;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

public class AggregatingPixExMeasurementFactory extends MeasurementFactory {

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
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        final float[][] values = new float[rasterNames.length][numPixels];

        for (int i = 0; i < rasterNames.length; i++) {
            String rasterName = rasterNames[i];
            float[] bandValues = new float[numPixels];
            final Band band = product.getBand(rasterName);
            setBandValues(product, band, bandValues, windowSize, pixelX, pixelY);
            values[i] = bandValues;
        }

        Record record = new DefaultRecord(values);

        final RecordTransformer recordAggregator = new RecordAggregator(-1, -1.0);
        final Record transformedRecord = recordAggregator.transform(record);

        final float[] numbers = new float[rasterNames.length];
        for (int i = 0; i < rasterNames.length; i++) {
            final Object[] attributeValues = transformedRecord.getAttributeValues();
            float valueForBand = aggregatorStrategy.getValue(attributeValues[i]);
            numbers[i] = valueForBand;
        }

        measurements[0] = createMeasurement(product, productId, coordinateID, coordinateName,
                                            createNumberArray(numbers), validData,
                                            pixelX, pixelY);
        return measurements;
    }

    private Number[] createNumberArray(float[] bandValues) {
        final Float[] result = new Float[bandValues.length];
        for (int i = 0; i < bandValues.length; i++) {
            result[i] = bandValues[i];
        }
        return result;
    }

    private Float[] createFloatArray(Number[] bandValues) {
        final Float[] result = new Float[bandValues.length];
        for (int i = 0; i < bandValues.length; i++) {
            Number bandValue = bandValues[i];
            result[i] = bandValue.floatValue();
        }
        return result;
    }

    protected static void setBandValues(Product product, RasterDataNode raster, float[] bandValues,
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
                            bandValues[pixelIndex] = temp == 0 ? 0 : 1; // normalize to 0 for false and 1 for true
                        } else {
                            if (raster.getDataType() == ProductData.TYPE_UINT32) {
                                bandValues[pixelIndex] = temp & 0xffffL;
                            } else {
                                bandValues[pixelIndex] = temp;
                            }
                        }
                    }
                }
                pixelIndex++;
            }
        }
    }

}
