package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Product;

/**
 * Used to prefix symbols derived from a product and registered in some {@link org.esa.snap.core.jexp.Namespace}.
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
