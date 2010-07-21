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

import java.io.IOException;

import javax.swing.JOptionPane;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.TextFieldXEditor;
import org.esa.beam.framework.param.validators.AbstractExpressionValidator;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.PropertyMap;

/**
 * An abstract editor for boolean and general expressions.
 */
public abstract class AbstractExpressionEditor extends TextFieldXEditor {
    //todo mp/** enable this editor to get a Namespace by a property

    public final static String PROPERTY_KEY_SELECTED_PRODUCT = AbstractExpressionValidator.PROPERTY_KEY_SELECTED_PRODUCT;
    public final static String PROPERTY_KEY_INPUT_PRODUCTS = AbstractExpressionValidator.PROPERTY_KEY_INPUT_PRODUCTS;
    public final static String PROPERTY_KEY_PREFERENCES = AbstractExpressionValidator.PROPERTY_KEY_PREFERENCES;

    protected AbstractExpressionEditor(final Parameter parameter) {
        super(parameter);
    }

    @Override
    protected void invokeXEditor() {

        final ParamProperties paramProperties = getParameter().getProperties();
        final P p;
        try {
            UIUtils.setRootFrameWaitCursor(getEditorComponent());
            p = getSelectedProduct(paramProperties);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getEditorComponent(),
                                          "Failed to invoke expression editor:\n" +
                                          e.getMessage(),      /*I18N*/
                                          "Expression Editor", /*I18N*/
                                          JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            UIUtils.setRootFrameDefaultCursor(getEditorComponent());
        }
        if (p == null) {
            JOptionPane.showMessageDialog(getEditorComponent(),
                                          "Failed to invoke expression editor:\n" +
                                          "No input product selected.", /*I18N*/
                                          "Expression Editor",          /*I18N*/
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        final Product[] sourceProducts = getInputProducts(paramProperties);
        final PropertyMap preferences = getPreferences(paramProperties);

        final ProductExpressionPane exprPane = createProductExpressionPane(sourceProducts != null ? sourceProducts: new Product[]{p.product},
                                                                           p.product,
                                                                           preferences);
        exprPane.setCode(getParameterExprValue());
        if (exprPane.showModalDialog(null, "Expression Editor") == ModalDialog.ID_OK) {
            setParameterExprValue(exprPane.getCode());
        }

        if (p.mustDispose) {
            p.product.dispose();
        }
        p.product = null;
    }

    private static PropertyMap getPreferences(final ParamProperties paramProperties) {
        final Object propertyValue = paramProperties.getPropertyValue(PROPERTY_KEY_PREFERENCES);
        if (propertyValue == null) {
            return null;
        }
        if (!(propertyValue instanceof PropertyMap)) {
            throw new IllegalStateException(
                    "parameter property '" + PROPERTY_KEY_PREFERENCES + "' is not instanceof PropertyMap");
        }
        return (PropertyMap) propertyValue;
    }

    private P getSelectedProduct(final ParamProperties paramProperties) throws IOException {
        final Object propertyValue = paramProperties.getPropertyValue(PROPERTY_KEY_SELECTED_PRODUCT);
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue instanceof Product) {
            return new P((Product) propertyValue, false);
        } else if (propertyValue instanceof String) {
            return new P(openProduct((String) propertyValue), true);
        } else {
            throw new IllegalStateException(
                    "parameter property '" + PROPERTY_KEY_SELECTED_PRODUCT + "' has an illegal type");
        }
    }

    private static Product[] getInputProducts(final ParamProperties paramProperties) {
        final Object propertyValue = paramProperties.getPropertyValue(PROPERTY_KEY_INPUT_PRODUCTS);
        if (propertyValue == null) {
            return null;
        }
        if (!(propertyValue instanceof Product[])) {
            throw new IllegalStateException(
                    "parameter property '" + PROPERTY_KEY_INPUT_PRODUCTS + "' is not instanceof Product[]");
        }
        final Product[] inputProducts = (Product[]) propertyValue;
        if (inputProducts.length == 0) {
            throw new IllegalStateException(
                    "parameter property '" + PROPERTY_KEY_INPUT_PRODUCTS + "' is an empty Product[]");
        }
        return inputProducts;
    }

    Product openProduct(final String path) throws IOException {
        return ProductIO.readProduct(path);
    }

    protected abstract ProductExpressionPane createProductExpressionPane(Product[] sourceProducts,
                                                                         Product currentProduct,
                                                                         PropertyMap preferences);

    private String getParameterExprValue() {
        return (String) getParameter().getValue();
    }

    private void setParameterExprValue(final String value) {
        getParameter().setValue(value, null);
    }

    private static class P {
        private Product product;
        private boolean mustDispose;

        public P(Product product, boolean mustClose) {
            this.product = product;
            this.mustDispose = mustClose;
        }
    }
}
