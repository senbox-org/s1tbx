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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.util.HashMap;
import java.util.Map;

public class SessionDomConverter extends DefaultDomConverter {

    private final Map<Class<?>, DomConverter> domConverterMap = new HashMap<Class<?>, DomConverter>(33);

    public SessionDomConverter(ProductManager productManager) {
        super(PropertyContainer.class);

        setDomConverter(Product.class, new ProductDomConverter(productManager));
        setDomConverter(RasterDataNode.class, new RasterDataNodeDomConverter(productManager));
        setDomConverter(BitmaskDef.class, new BitmaskDefDomConverter(productManager));
        setDomConverter(PlacemarkDescriptor.class, new PlacemarkDescriptorDomConverter());
    }

    final void setDomConverter(Class<?> type, DomConverter domConverter) {
        domConverterMap.put(type, domConverter);
    }

    @Override
    protected DomConverter getDomConverter(PropertyDescriptor descriptor) {
        DomConverter domConverter = getDomConverter(descriptor.getType());
        if (domConverter == null) {
            domConverter = super.getDomConverter(descriptor);
        }
        return domConverter;
    }

    private DomConverter getDomConverter(Class<?> type) {
        DomConverter domConverter = domConverterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = domConverterMap.get(type);
        }
        return domConverter;
    }
}
