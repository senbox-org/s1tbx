package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.MeasurementFactory;
import org.esa.beam.util.ProductUtils;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PixExMeasurementFactory implements MeasurementFactory {

    private final RasterNamesFactory rasterNamesFactory;
    private final int windowSize;
    private final ProductRegistry productRegistry;

    public PixExMeasurementFactory(final RasterNamesFactory rasterNamesFactory,
                                   final int windowSize, final ProductRegistry productRegistry) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.windowSize = windowSize;
        this.productRegistry = productRegistry;
    }

    @Override
    public Measurement[] createMeasurements(int centerX, int centerY, int coordinateID, String coordinateName,
                                            Product product, Raster validData) throws IOException {
        final long productId = productRegistry.getProductId(product);
        final int numPixels = windowSize * windowSize;
        final List<Measurement> measurements = new ArrayList<Measurement>();
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        final Number[] values = new Number[rasterNames.length];
        Arrays.fill(values, Double.NaN);
        final int windowBorder = windowSize / 2;

        for (int idx = 0; idx < numPixels; idx++) {
            final int offsetX = idx % windowSize;
            final int offsetY = idx / windowSize;
            int x = centerX - windowBorder + offsetX;
            int y = centerY - windowBorder + offsetY;

            measurements.add(createMeasurement(product, productId,
                                               coordinateID, coordinateName,
                                               rasterNames, values, validData, x, y)
            );
        }

        return measurements.toArray(new Measurement[measurements.size()]);
    }

    private static Measurement createMeasurement(Product product, long productId,
                                                 int coordinateID,
                                                 String coordinateName, String[] rasterNames, Number[] values,
                                                 Raster validData, int x, int y) throws IOException {
        for (int i = 0; i < rasterNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
            if (raster != null && product.containsPixel(x, y)) {
                if (!raster.isPixelValid(x, y)) {
                    values[i] = Double.NaN;
                } else if (raster.isFloatingPointType()) {
                    values[i] = (double) raster.getSampleFloat(x, y);
                } else {
                    int temp = raster.getSampleInt(x,y);
                    if (raster instanceof Mask) {
                        values[i] = temp == 0 ? 0 : 1; // normalize to 0 for false and 1 for true
                    } else {
                        if (raster.getDataType() == ProductData.TYPE_UINT32) {
                            values[i] = temp & 0xffffL;
                        } else {
                            values[i] = temp;
                        }
                    }
                }
            }
        }
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final GeoCoding geoCoding = product.getGeoCoding();
        final GeoPos currentGeoPos;
        if (geoCoding != null) {
            currentGeoPos = geoCoding.getGeoPos(pixelPos, null);
        } else {
            currentGeoPos = new GeoPos();
        }
        final boolean isValid = validData == null || validData.getSample(x, y, 0) != 0;
        final ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, pixelPos.y);

        return new Measurement(coordinateID, coordinateName, productId,
                               pixelPos.x, pixelPos.y, scanLineTime, currentGeoPos, values,
                               isValid);
    }
}
