package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class ProductLoop {

    private final ProductLoader loader;
    private final Logger logger;
    private final StatisticComputer statisticComputer;
    private int numComputations;

    public ProductLoop(ProductLoader loader, StatisticComputer statisticComputer, Logger logger) {
        this.loader = loader;
        this.logger = logger;
        this.statisticComputer = statisticComputer;
        numComputations = 0;
    }

    public void loop(Product[] alreadyLoadedProducts, File[] productFilesToLoad) {
        for (Product product : alreadyLoadedProducts) {
            compute(product);
        }
        for (File productFile : productFilesToLoad) {
            if (isProductAlreadyOpened(alreadyLoadedProducts, productFile)) {
                continue;
            }
            loadProductAndCompute(productFile);
        }
        if (numComputations == 0) {
            throw new OperatorException("No input products found.");
        }
    }

    private void loadProductAndCompute(File productFile) {
        try {
            final Product product = loader.loadProduct(productFile);
            if (product == null) {
                logReadProductError(productFile);
                return;
            }
            try {
                compute(product);
            } finally {
                product.dispose();
            }
        } catch (IOException e) {
            logReadProductError(productFile);
        }
    }

    private void compute(Product product) {
        statisticComputer.computeStatistic(product);
        numComputations++;
    }

    private void logReadProductError(File productFile) {
        logger.severe(String.format("Failed to read from '%s' (not a data product or reader missing)", productFile));
    }

    static boolean isProductAlreadyOpened(Product[] alreadyLoadedProducts, File file) {
        for (Product product : alreadyLoadedProducts) {
            if (product.getFileLocation().getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }
}
