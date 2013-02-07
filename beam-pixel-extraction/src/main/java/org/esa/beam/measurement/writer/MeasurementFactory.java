package org.esa.beam.measurement.writer;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;

import java.awt.image.Raster;
import java.io.IOException;

public interface MeasurementFactory {

    Measurement[] createMeasurements(int pixelX, int pixelY, int coordinateID, String coordinateName,
                                     Product product, Raster validData) throws IOException;

    void close();
}
