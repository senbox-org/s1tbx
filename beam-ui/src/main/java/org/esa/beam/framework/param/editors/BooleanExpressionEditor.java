/*
 * $Id: BooleanExpressionEditor.java,v 1.1 2006/10/10 14:47:36 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
