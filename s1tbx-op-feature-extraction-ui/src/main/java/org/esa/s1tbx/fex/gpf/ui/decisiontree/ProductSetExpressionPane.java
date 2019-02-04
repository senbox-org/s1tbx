package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductExpressionPane;

import java.util.Arrays;

/**
 * ExpressionPane to support product sets
 */
public class ProductSetExpressionPane extends ProductExpressionPane {

    private Product[] products;
    private ProductSetNamespace namespaceManager;

    protected ProductSetExpressionPane(boolean booleanExpr,
                                       Product[] products,
                                       Product currentProduct,
                                       PropertyMap preferences) {
        super(booleanExpr, products, currentProduct, preferences);
        this.products = products;
    }

    protected Namespace createNamespace() {
        final int defaultIndex = Arrays.asList(products).indexOf(getCurrentProduct());
        if (namespaceManager == null) {
            namespaceManager = new ProductSetNamespace(products);
        }
        return namespaceManager.createNamespace(defaultIndex == -1 ? 0 : defaultIndex);
    }

    protected String getNodeNamePrefix() {
        final String namePrefix;
        if (products.length > 1) {
            namePrefix = namespaceManager.getProductNodeNamePrefix(getCurrentProduct());
        } else {
            namePrefix = "";
        }
        return namePrefix;
    }


}
