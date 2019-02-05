package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.ProductNamespacePrefixProvider;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.util.Guardian;

/**
 * Namespace for product set
 */
public class ProductSetNamespace {

    private final Product[] sourceProducts;

    public ProductSetNamespace(final Product[] products) {
        this.sourceProducts = products;
    }

    public Namespace createNamespace(int defaultProductIndex) {
        return BandArithmetic.createDefaultNamespace(sourceProducts, defaultProductIndex,

                new ProductNamespacePrefixProvider() {
                    @Override
                    public String getPrefix(Product product) {
                        return getProductNodeNamePrefix(product);
                    }
                });
    }

    /**
     * Gets a symbol name prefix for the names of bands, tie point grids, flags, etc. of the given product.
     * The prefix is of the general form <code>"$<i>refNo</i>."</code> where <i>refNo</i> is the product's reference
     * number returned by {@link org.esa.snap.core.datamodel.Product#getRefNo()}.
     *
     * @param product the product, must not be <code>null</code>
     * @return a node name prefix, never null.
     */
    public String getProductNodeNamePrefix(Product product) {
        Guardian.assertNotNull("product", product);
        //return "$product" + product.getRefNo() + '.';
        return "$" + indexOf(product) + '.';
    }

    private int indexOf(final Product product) {
        for (int i = 0; i < sourceProducts.length; ++i) {
            if (product.equals(sourceProducts[i])) {
                return i;
            }
        }
        return 0;
    }
}
