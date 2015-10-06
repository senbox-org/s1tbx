package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;

import java.io.IOException;

public interface ProductRegistry {

    long getProductId(Product product) throws IOException;

    void close();
}
