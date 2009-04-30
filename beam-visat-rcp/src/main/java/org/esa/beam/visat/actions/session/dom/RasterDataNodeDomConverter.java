package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;

class RasterDataNodeDomConverter extends ProductNodeDomConverter<RasterDataNode> {

    RasterDataNodeDomConverter(ProductManager productManager) {
        super(RasterDataNode.class, productManager);
    }

    @Override
    protected RasterDataNode getProductNode(DomElement parentElement, Product product) {
        final DomElement rasterName = parentElement.getChild("rasterName");
        return product.getRasterDataNode(rasterName.getValue());
    }

    @Override
    protected void convertProductNodeToDom(RasterDataNode raster, DomElement parentElement) {
        final DomElement rasterName = parentElement.createChild("rasterName");
        rasterName.setValue(raster.getName());
    }
}
