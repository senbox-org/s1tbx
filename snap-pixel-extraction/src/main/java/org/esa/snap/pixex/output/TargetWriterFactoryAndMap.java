package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TargetWriterFactoryAndMap {

    private static final String MEASUREMENTS_FILE_NAME_PATTERN = "%s_%s_measurements.txt";

    private final Map<String, PrintWriter> writerMap;
    private final String filenamePrefix;
    private final File outputDir;

    public TargetWriterFactoryAndMap(final String filenamePrefix, final File outputDir) {
        writerMap = new HashMap<String, PrintWriter>();
        this.filenamePrefix = filenamePrefix;
        this.outputDir = outputDir;
    }

    public boolean containsWriterFor(Product product) {
        return writerMap.containsKey(product.getProductType());
    }

    public PrintWriter getWriterFor(Product product) {
        return writerMap.get(product.getProductType());
    }

    public PrintWriter createWriterFor(Product product) throws IOException {
        String productType = product.getProductType();
        final String fileName = String.format(MEASUREMENTS_FILE_NAME_PATTERN, filenamePrefix, productType);
        File coordinateFile = new File(outputDir, fileName);
        PrintWriter writer = new PrintWriter(new FileOutputStream(coordinateFile));
        writerMap.put(productType, writer);
        return writer;
    }

    public void close() {
        final Collection<PrintWriter> writerCollection = writerMap.values();
        for (PrintWriter printWriter : writerCollection) {
            printWriter.close();
            //noinspection SuspiciousMethodCalls
//            writerMap.remove(printWriter);
        }
    }
}
