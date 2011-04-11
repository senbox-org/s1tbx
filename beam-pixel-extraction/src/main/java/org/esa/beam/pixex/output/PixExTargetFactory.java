package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.writer.TargetFactory;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PixExTargetFactory implements TargetFactory {

    private static final String MEASUREMENTS_FILE_NAME_PATTERN = "%s_%s_measurements.txt";

    private final Map<String, PrintWriter> writerMap;
    private final String filenamePrefix;
    private final File outputDir;

    public PixExTargetFactory(final String filenamePrefix, final File outputDir) {
        writerMap = new HashMap<String, PrintWriter>();
        this.filenamePrefix = filenamePrefix;
        this.outputDir = outputDir;
    }

    @Override
    public boolean containsWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData) {
        return writerMap.containsKey(product.getProductType());
    }

    @Override
    public PrintWriter getWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData) {
        return writerMap.get(product.getProductType());
    }

    @Override
    public PrintWriter createWriterFor(int pixelX, int pixelY, int coordinateID, String coordinateName, Product product, Raster validData) throws IOException {
        String productType = product.getProductType();
        final String fileName = String.format(MEASUREMENTS_FILE_NAME_PATTERN, filenamePrefix, productType);
        File coordinateFile = new File(outputDir, fileName);
        PrintWriter writer = new PrintWriter(new FileOutputStream(coordinateFile));
        writerMap.put(productType, writer);
        return writer;
    }

    @Override
    public void close() {
        final Collection<PrintWriter> writerCollection = writerMap.values();
        for (PrintWriter printWriter : writerCollection) {
            printWriter.close();
        }
    }
}
