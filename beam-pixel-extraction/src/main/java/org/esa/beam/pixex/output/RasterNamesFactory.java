package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;

public interface RasterNamesFactory {

    String[] getRasterNames(final Product product);

    String[] getUniqueRasterNames(Product product);
}
