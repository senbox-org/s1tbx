package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

class ProductDomConverter extends ProductNodeDomConverter<Product> {

    ProductDomConverter(ProductManager productManager) {
        super(Product.class, productManager);
    }

    @Override
    protected Product getProductNode(DomElement parentElement, Product product) {
        return product;
    }

    @Override
    protected void convertProductNodeToDom(Product product, DomElement parentElement) {
    }
}
