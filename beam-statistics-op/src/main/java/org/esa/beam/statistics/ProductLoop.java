package org.esa.beam.statistics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;

public class ProductLoop {

    private final ProductLoader loader;
    private final Logger logger;
    private final StatisticComputer statisticComputer;
    private List<String> productNames;
    private final ProductData.UTC startDate;
    private final ProductData.UTC endDate;
    private ProductData.UTC newestDate;
    private ProductData.UTC oldestDate;
    private final ProductValidator productValidator;

    public ProductLoop(ProductLoader loader, ProductValidator productValidator, StatisticComputer statisticComputer, ProductData.UTC startDate, ProductData.UTC endDate, Logger logger) {
        this.loader = loader;
        this.productValidator = productValidator;
        this.logger = logger;
        this.statisticComputer = statisticComputer;
        productNames = new ArrayList<String>();
        this.startDate = startDate == null ? new ProductData.UTC(0) : startDate;
        this.endDate = endDate == null ? new ProductData.UTC(Long.MAX_VALUE) : endDate;
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
        if (productNames.size() == 0) {
            throw new OperatorException("No input products found.");
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
        statisticComputer.computeStatistic(product);
        productNames.add(product.getName());
        System.out.println(productNames.size() + " computed");
        System.out.println("   current product: " + product.getFileLocation().getAbsolutePath());
    }

    private boolean isInDateRange(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        if (startTime == null && endTime == null) {
            return true;
        }
        return startDate.getAsDate().getTime() <= startTime.getAsDate().getTime()
               && endDate.getAsDate().getTime() >= endTime.getAsDate().getTime();
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
                if (product.getFileLocation().getAbsolutePath().equals(file.getAbsolutePath())) {
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
