package org.esa.beam.statistics;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;

public class ProductLoader {

    public Product loadProduct(File path) throws IOException {
        return ProductIO.readProduct(path);
    }
}
