package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ProductLoop {

    private final ProductLoader loader;
    private final Logger logger;
    private final StatisticComputer statisticComputer;
    private List<String> productNames;
    private ProductData.UTC newestDate;
    private ProductData.UTC oldestDate;
    private final ProductValidator productValidator;

    public ProductLoop(ProductLoader loader, ProductValidator productValidator, StatisticComputer statisticComputer, Logger logger) {
        this.loader = loader;
        this.productValidator = productValidator;
        this.logger = logger;
        this.statisticComputer = statisticComputer;
        productNames = new ArrayList<>();
        newestDate = null;
        oldestDate = null;
    }

    public void loop(Product[] alreadyLoadedProducts, File[] productFilesToLoad) {
        if (alreadyLoadedProducts != null) {
            for (Product product : alreadyLoadedProducts) {
                compute(product);
            }
        }
        for (File productFile : productFilesToLoad) {
            if (productFile == null) {
                continue;
            }
            if (isProductAlreadyOpened(alreadyLoadedProducts, productFile)) {
                continue;
            }
            loadProductAndCompute(productFile);
        }
    }

    public String[] getProductNames() {
        return productNames.toArray(new String[productNames.size()]);
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
        if (product == null || !productValidator.isValid(product)) {
            return;
        }
        final String path;
        if (product.getFileLocation() != null) {
            path = product.getFileLocation().getAbsolutePath();
        } else {
            path = product.getName();
        }
        logger.info("    current product: " + path);

        statisticComputer.computeStatistic(product);
        productNames.add(path);

        logger.fine("    " + productNames.size() + " computed:");
        logger.fine("        product: " + path);
    }

    private void logReadProductError(File productFile) {
        logger.severe(String.format("Failed to read from '%s' (not a data product or reader missing)", productFile));
    }

    static boolean isProductAlreadyOpened(Product[] alreadyLoadedProducts, File file) {
        if (alreadyLoadedProducts != null) {
            for (Product product : alreadyLoadedProducts) {
                if (product == null) {
                    continue;
                }
                final File fileLocation = product.getFileLocation();
                if (fileLocation == null) {
                    continue;
                }
                if (fileLocation.getAbsolutePath().equals(file.getAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public ProductData.UTC getNewestDate() {
        return newestDate;
    }

    public ProductData.UTC getOldestDate() {
        return oldestDate;
    }
}
