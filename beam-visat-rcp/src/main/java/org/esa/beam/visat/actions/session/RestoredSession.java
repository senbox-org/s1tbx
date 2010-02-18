package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductNodeView;

/**
 * A restored session comprising products and views.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class RestoredSession {

    private final Product[] products;
    private final ProductNodeView[] views;
    private final Exception[] problems;

    public RestoredSession(Product[] products, ProductNodeView[] views, Exception[] problems) {
        this.products = products;
        this.views = views;
        this.problems = problems;
    }

    public Product[] getProducts() {
        return products.clone();
    }

    public ProductNodeView[] getViews() {
        return views.clone();
    }

    public Exception[] getProblems() {
        return problems.clone();
    }
}
