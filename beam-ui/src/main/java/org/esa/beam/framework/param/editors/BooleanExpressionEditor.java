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
package org.esa.beam.framework.param.editors;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.PropertyMap;

/**
 * An editor for expressions which return a boolean value.
 */
public class BooleanExpressionEditor extends AbstractExpressionEditor {

    public BooleanExpressionEditor(final Parameter parameter) {
        super(parameter);
    }

    @Override
    protected ProductExpressionPane createProductExpressionPane(final Product[] sourceProducts,
                                                                final Product currentProduct,
                                                                final PropertyMap preferences) {
        return ProductExpressionPane.createBooleanExpressionPane(sourceProducts, currentProduct, preferences);
    }
}
