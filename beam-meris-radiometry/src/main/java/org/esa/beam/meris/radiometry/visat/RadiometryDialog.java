/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.meris.radiometry.visat;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.OperatorMenuSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.ArrayUtils;

import javax.swing.JOptionPane;
import java.util.HashMap;

class RadiometryDialog extends SingleTargetProductDialog {

    static final String N1_FORMAT = EnvisatConstants.ENVISAT_FORMAT_NAME + " N1";

    private String alias;
    private RadiometryForm form;
    private HashMap<String, Object> parameterMap;

    RadiometryDialog(String alias, AppContext appContext, String title, String helpId) {
        super(appContext, title, ID_APPLY_CLOSE_HELP, helpId,
              TargetProductSelectorModel.createEnvisatTargetProductSelectorModel(N1_FORMAT));
        this.alias = alias;
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(alias);
        parameterMap = new HashMap<String, Object>(17);
        PropertyContainer propContainer = PropertyContainer.createMapBacked(parameterMap,
                                                                            operatorSpi.getOperatorClass(),
                                                                            new ParameterDescriptorFactory());
        propContainer.setDefaultValues();
        form = new RadiometryForm(appContext, operatorSpi, propContainer, getTargetProductSelector());
        OperatorMenuSupport menuSupport = new OperatorMenuSupport(this.getJDialog(),
                                                                  operatorSpi.getOperatorClass(),
                                                                  propContainer,
                                                                  helpId);
        getJDialog().setJMenuBar(menuSupport.createDefaultMenue());

    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        final Product radioCorrProduct = GPF.createProduct(alias, parameterMap, sourceProduct);
        if (isEnvisatFormatSelected() && getTargetProductSelector().getModel().isSaveToFileSelected()) {
            final HashMap<String, Object> n1Parameters = new HashMap<String, Object>();
            n1Parameters.put("patchedFile", getTargetProductSelector().getModel().getProductFile());
            final HashMap<String, Product> sourceProductMap = new HashMap<String, Product>();
            sourceProductMap.put("n1", form.getSourceProduct());
            sourceProductMap.put("input", radioCorrProduct);
            return GPF.createProduct("N1Patcher", n1Parameters, sourceProductMap);
        }
        return radioCorrProduct;
    }

    @Override
    protected void onApply() {
        if (validateUserInput()) {
            super.onApply();
        }

    }

    private boolean validateUserInput() {
        if (isEnvisatFormatSelected()) {
            final ProductReader productReader = form.getSourceProduct().getProductReader();
            boolean isEnvisatSource = false;
            if (productReader != null) {
                final String[] formatNames = productReader.getReaderPlugIn().getFormatNames();
                isEnvisatSource = ArrayUtils.getElementIndex(EnvisatConstants.ENVISAT_FORMAT_NAME, formatNames) != -1;
            }
            if (!isEnvisatSource) {
                final String msg = "If " + EnvisatConstants.ENVISAT_FORMAT_NAME + " is selected as output format, " +
                                   "the source product must be in the same format.";
                JOptionPane.showMessageDialog(this.getContent(), msg, "Invalid Settings", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    private boolean isEnvisatFormatSelected() {
        return EnvisatConstants.ENVISAT_FORMAT_NAME.equals(getTargetProductSelector().getModel().getFormatName());
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
