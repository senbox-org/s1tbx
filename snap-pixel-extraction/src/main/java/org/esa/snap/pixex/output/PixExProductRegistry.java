package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class PixExProductRegistry implements ProductRegistry {

    private static final String PRODUCT_MAP_FILE_NAME_PATTERN = "%s_productIdMap.txt";

    private final List<ProductIdentifier> productList;
    private final String filenamePrefix;
    private final File outputDir;

    private PrintWriter productMapWriter;

    public PixExProductRegistry(final String filenamePrefix, final File outputDir) {
        productList = new ArrayList<ProductIdentifier>();
        this.filenamePrefix = filenamePrefix;
        this.outputDir = outputDir;
    }


    @Override
    public long getProductId(Product product) throws IOException {
        final ProductIdentifier identifier = ProductIdentifier.create(product);
        if (!productList.contains(identifier)) {
            if (productMapWriter == null) {
                productMapWriter = createProductMapWriter();
            }
            productList.add(identifier);

            final String productType = product.getProductType();

            productMapWriter.printf("%d\t%s\t%s%n", productList.indexOf(identifier), productType,
                                    identifier.getLocation());

            if (productMapWriter.checkError()) {
                throw new IOException("Error occurred while writing measurement.");
            }

        }
        return productList.indexOf(identifier);
    }

    @Override
    public void close() {
        if (productMapWriter != null) {
            productMapWriter.close();
        }
    }

    private PrintWriter createProductMapWriter() throws FileNotFoundException {

        final String fileName = String.format(PRODUCT_MAP_FILE_NAME_PATTERN, filenamePrefix);
        final File file = new File(outputDir, fileName);
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file), true);
        writeProductMapHeader(printWriter);

        return printWriter;
    }

    private void writeProductMapHeader(PrintWriter printWriter) {
        printWriter.printf("# Product ID Map%n");
        printWriter.printf("ProductID\tProductType\tProductLocation%n");
    }

}
