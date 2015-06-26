package org.esa.snap.framework.dataop.barithm;

import org.esa.snap.framework.datamodel.Product;

/**
 * Used to prefix symbols derived from a product and registered in some {@link com.bc.jexp.Namespace}.
 * SNAP's default prefix is {@code "$<ref-no>."}.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public interface ProductNamespacePrefixProvider {

    /**
     * @param product The product.
     * @return The product prefix.
     */
    String getPrefix(Product product);
}
