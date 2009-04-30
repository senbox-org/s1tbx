package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

class BitmaskDefDomConverter extends ProductNodeDomConverter<BitmaskDef> {

    BitmaskDefDomConverter(ProductManager productManager) {
        super(BitmaskDef.class, productManager);
    }

    @Override
    protected BitmaskDef getProductNode(DomElement parentElement, Product product) {
        final String bitmaskName = parentElement.getChild("bitmaskName").getValue();
        return product.getBitmaskDef(bitmaskName);
    }

    @Override
    protected void convertProductNodeToDom(BitmaskDef bitmaskDef, DomElement parentElement) {
        final DomElement bitmaskName = parentElement.createChild("bitmaskName");
        bitmaskName.setValue(bitmaskDef.getName());
    }
}
