package org.esa.snap.measurement.writer;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;

import java.awt.image.Raster;
import java.io.IOException;

public interface MeasurementFactory {

    Measurement[] createMeasurements(int pixelX, int pixelY, int coordinateID, String coordinateName,
                                     Product product, Raster validData) throws IOException;

    void close();
}
