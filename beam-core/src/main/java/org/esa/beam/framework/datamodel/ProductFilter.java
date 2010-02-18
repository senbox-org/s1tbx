package org.esa.beam.framework.datamodel;

/**
 * A filter for products.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public interface ProductFilter {

    /**
     * @param product The product.
     * @return {@code true}, if the given {@code product} is accepted by this filter.
     */
    boolean accept(Product product);
}
