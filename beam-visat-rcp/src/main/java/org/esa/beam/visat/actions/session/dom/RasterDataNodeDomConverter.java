/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
