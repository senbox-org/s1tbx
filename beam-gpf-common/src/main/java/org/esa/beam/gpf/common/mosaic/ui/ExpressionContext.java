package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.framework.datamodel.Product;

interface ExpressionContext {

    Product getProduct();

    void addListener(Listener listener);

    interface Listener {

        void contextChanged(ExpressionContext context);
    }
}
