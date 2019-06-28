package org.esa.snap.remote.execution.machines.executors;

import java.nio.file.Path;

/**
 * Created by jcoravu on 29/5/2019.
 */
public class SlaveProductsInputData {

    private final Path outputRootFolder;
    private final String productsFolderNamePrefix;
    private final String productsName;
    private final String productsFormatName;

    public SlaveProductsInputData(Path outputRootFolder, String productsFolderNamePrefix, String productsName, String productsFormatName) {
        this.outputRootFolder = outputRootFolder;

        this.productsFolderNamePrefix = productsFolderNamePrefix;//"output";
        this.productsName = productsName; // the output product name cannot be null
        this.productsFormatName = productsFormatName;//"BEAM-DIMAP";
    }

    public Path getOutputRootFolder() {
        return outputRootFolder;
    }

    public String getProductsFolderNamePrefix() {
        return productsFolderNamePrefix;
    }

    public String getProductsFormatName() {
        return productsFormatName;
    }

    public String getProductsName() {
        return productsName;
    }
}
