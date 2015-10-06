package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PixExMeasurementFactory extends AbstractMeasurementFactory {

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
        final List<Measurement> measurements = new ArrayList<>();
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        final Number[] values = new Number[rasterNames.length];
        final int windowBorder = windowSize / 2;

        for (int idx = 0; idx < numPixels; idx++) {
            final int offsetX = idx % windowSize;
            final int offsetY = idx / windowSize;
            int x = centerX - windowBorder + offsetX;
            int y = centerY - windowBorder + offsetY;

            setBandValues(product, rasterNames, x, y, values);
            final boolean isValid = validData == null || validData.getSample(x, y, 0) != 0;
            measurements.add(createMeasurement(product, productId, coordinateID, coordinateName, values, isValid, x, y));
        }

        return measurements.toArray(new Measurement[measurements.size()]);
    }

    @Override
    public void close() {
        productRegistry.close();
    }
}
