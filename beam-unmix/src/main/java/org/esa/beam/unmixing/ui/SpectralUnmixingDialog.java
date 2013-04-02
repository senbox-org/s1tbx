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
package org.esa.beam.unmixing.ui;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.ParameterUpdater;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.Map;


public class SpectralUnmixingDialog extends SingleTargetProductDialog {

    public static final String HELP_ID = "spectralUnmixing";
    private static final String TITLE = "Spectral Unmixing";

    private final SpectralUnmixingForm form;
    private final OperatorParameterSupport parameterSupport;

    public SpectralUnmixingDialog(AppContext appContext) {
        super(appContext, TITLE, ID_APPLY_CLOSE, HELP_ID);

        ParameterUpdater parameterUpdater = new ParameterUpdater() {

            @Override
            public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
                updateParameterMap(parameterMap);
            }

            @Override
            public void handleParameterLoadRequest(Map<String, Object> parameterMap) {
                updateEndmemberFormModel(parameterMap);
            }
        };

        parameterSupport = new OperatorParameterSupport(SpectralUnmixingOp.class, null, null, parameterUpdater);
        form = new SpectralUnmixingForm(appContext, parameterSupport.getPopertySet(), getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     SpectralUnmixingOp.class,
                                                     parameterSupport,
                                                     HELP_ID);

        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    private void updateEndmemberFormModel(Map<String, Object> parameterMap) {
        Object endmembers = parameterMap.get("endmembers");
        if (endmembers instanceof Endmember[]) {
            EndmemberFormModel endmemberFormModel = form.getEndmemberForm().getFormModel();
            endmemberFormModel.setEndmembers((Endmember[]) endmembers);
        }
    }

    private void updateParameterMap(Map<String, Object> parameterMap) {
        parameterMap.put("endmembers", form.getEndmemberForm().getFormModel().getEndmembers());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final SpectralUnmixingFormModel formModel = form.getFormModel();
        Map<String, Object> parameterMap = parameterSupport.getParameterMap();
        updateParameterMap(parameterMap);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                                 parameterMap,
                                 formModel.getSourceProduct());
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

    @Override
    protected boolean verifyUserInput() {
        final SpectralUnmixingFormModel formModel = form.getFormModel();
        if (formModel.getSourceProduct() == null) {
            showErrorDialog("No source product selected.");
            return false;
        }
        final Map<String, Object> parameterMap = parameterSupport.getParameterMap();
        updateParameterMap(parameterMap);

        final Endmember[] endmembers = (Endmember[]) parameterMap.get("endmembers");
        final String[] sourceBandNames = (String[]) parameterMap.get("sourceBandNames");
        final double minBandwidth = (Double) parameterMap.get("minBandwidth");

        double[] sourceWavelengths = new double[sourceBandNames.length];
        double[] sourceBandwidths = new double[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final Band sourceBand = formModel.getSourceProduct().getBand(sourceBandNames[i]);
            sourceWavelengths[i] = sourceBand.getSpectralWavelength();
            sourceBandwidths[i] = sourceBand.getSpectralBandwidth();
        }
        if (!matchingWavelength(endmembers, sourceWavelengths, sourceBandwidths, minBandwidth)) {
            showErrorDialog("One or more source wavelengths do not fit\n" +
                            "to one or more endmember spectra.\n\n" +
                            "Consider increasing the maximum wavelength deviation.");
            return false;
        }

        return true;
    }

    private static boolean matchingWavelength(Endmember[] endmembers,
                                              double[] sourceWavelengths,
                                              double[] sourceBandwidths,
                                              double minBandwidth) {
        for (Endmember endmember : endmembers) {
            final double[] endmemberWavelengths = endmember.getWavelengths();
            for (int i = 0; i < sourceWavelengths.length; i++) {
                double sourceWavelength = sourceWavelengths[i];
                double sourceBandwidth = sourceBandwidths[i];
                final int k = SpectralUnmixingOp.findEndmemberSpectralIndex(endmemberWavelengths, sourceWavelength,
                                                                            Math.max(sourceBandwidth, minBandwidth));
                if (k == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException,
                                                  InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        float[] wl = new float[]{
                412.6395569f,
                442.5160217f,
                489.8732910f,
                509.8299866f,
                559.7575684f,
                619.7247925f,
                664.7286987f,
                680.9848022f,
                708.4989624f,
                753.5312500f,
                761.7092285f,
                778.5520020f,
                864.8800049f,
                884.8975830f,
                899.9100342f
        };
        final Product inputProduct = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = inputProduct.addBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
        }
        inputProduct.addBand("l1_flags", ProductData.TYPE_UINT32);

        DefaultAppContext context = new DefaultAppContext("dev0");
        context.getProductManager().addProduct(inputProduct);
        context.setSelectedProduct(inputProduct);
        SpectralUnmixingDialog dialog = new SpectralUnmixingDialog(context);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }


}
