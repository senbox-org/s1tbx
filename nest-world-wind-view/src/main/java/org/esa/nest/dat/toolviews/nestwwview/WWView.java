package org.esa.nest.dat.toolviews.nestwwview;

import org.esa.beam.framework.datamodel.Product;

/**
 * Interface for WorldWind ToolView
 */
public interface WWView {


    public void setSelectedProduct(final Product product);

    public void setProducts(Product[] products);

    public void removeProduct(Product product);
}
