package org.esa.snap.pixex.output;

import org.esa.snap.framework.datamodel.Product;

public interface RasterNamesFactory {

    String[] getRasterNames(final Product product);

    String[] getUniqueRasterNames(Product product);
}
