package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

public interface ProductRegistry {

    long getProductId(Product product) throws IOException;

    void close();
}
