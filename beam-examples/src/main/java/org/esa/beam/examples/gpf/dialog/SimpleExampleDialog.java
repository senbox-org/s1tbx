/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.examples.gpf.dialog;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

/**
 * Date: 13.07.11
 */
public class SimpleExampleDialog extends SingleTargetProductDialog {

    private String alias;
    private OperatorParameterSupport parameterSupport;
    private SimpleForm form;

    public SimpleExampleDialog(String alias, AppContext appContext, String title, String helpId) {
        super(appContext, title, helpId);

        this.alias = alias;
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(alias);

        parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorClass());
        form = new SimpleForm(appContext, operatorSpi, parameterSupport.getPropertySet(),
                              getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorClass(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpId);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        return GPF.createProduct(alias, parameterSupport.getParameterMap(), sourceProduct);
    }

    @Override
    protected void onApply() {
        if (validateUserInput()) {
            super.onApply();
        }

    }

    private boolean validateUserInput() {
        return true;
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

}
