package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.WildcardMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductLoader {

    private final String[] sourceProductPaths;
    private final ProductValidator productValidator;
    private final Logger logger;

    public ProductLoader(String[] sourceProductPaths, ProductValidator productValidator, Logger logger) {
        this.sourceProductPaths = sourceProductPaths;
        this.productValidator = productValidator;
        this.logger = logger;
    }

    public Product[] loadProducts() {
        final ArrayList<Product> products = new ArrayList<Product>();
        for (String sourceProductPath : sourceProductPaths) {
            final File[] files;
            try {
                files = WildcardMatcher.glob(sourceProductPath);
            } catch (IOException e) {
                logger.severe("'" + sourceProductPath + "' is not a valid products wildcard path.");
                logger.log(Level.SEVERE, e.getMessage(), e);
                continue;
            }
            for (File productFile : files) {
                final Product product;
                try {
                    logger.info("Trying to open product file '" + productFile.getAbsolutePath() + "'.");
                    product = loadProduct(productFile);
                } catch (IOException e) {
                    logger.severe("Unable to read product '" + productFile.getAbsolutePath() + "'.");
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    continue;
                }
                if (productValidator.isValid(product)) {
                    products.add(product);
                } else {
                    if (product != null) {
                        product.dispose();
                    }
                }
            }
        }
        return products.toArray(new Product[products.size()]);
    }

    private Product loadProduct(File path) throws IOException {
        try {
            return ProductIO.readProduct(path);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
