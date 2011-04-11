package org.esa.beam.measurement.writer;

import org.esa.beam.framework.datamodel.Product;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.PrintWriter;

public interface TargetFactory {

    boolean containsWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData);

    PrintWriter getWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData);

    PrintWriter createWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData) throws IOException;

    void close();
}
