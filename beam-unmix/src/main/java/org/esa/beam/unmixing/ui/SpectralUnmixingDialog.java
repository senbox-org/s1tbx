/*
 * $Id$
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.esa.beam.unmixing.ui;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.util.PropertyMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Window;
import java.util.Map;


public class SpectralUnmixingDialog extends SingleTargetProductDialog {
    private SpectralUnmixingForm form;
    private static final String TITLE = "Spectral Unmixing";

    public SpectralUnmixingDialog(AppContext appContext) {
        super(appContext, TITLE, "spectralUnmixing");
        form = new SpectralUnmixingForm(appContext, getTargetProductSelector());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final SpectralUnmixingFormModel formModel = form.getFormModel();
        formModel.getOperatorParameters().put("endmembers", form.getEndmemberForm().getFormModel().getEndmembers());
        return GPF.createProduct(SpectralUnmixingOp.Spi.class.getName(),
                                 formModel.getOperatorParameters(),
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
        final Map<String, Object> parameters = formModel.getOperatorParameters();
        parameters.put("endmembers", form.getEndmemberForm().getFormModel().getEndmembers());

        final Endmember[] endmembers = (Endmember[]) parameters.get("endmembers");
        final String[] sourceBandNames = (String[]) parameters.get("sourceBandNames");
        final double minBandwidth = (Double) parameters.get("minBandwidth");

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
                final int k = SpectralUnmixingOp.findEndmemberSpectralIndex(endmemberWavelengths, sourceWavelength, Math.max(sourceBandwidth, minBandwidth));
                if (k == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final Product inputProduct = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < 15; i++) {
            Band band = inputProduct.addBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(500 + i * 30);
            band.setSpectralBandIndex(i);
        }
        inputProduct.addBand("l1_flags", ProductData.TYPE_UINT32);

        SpectralUnmixingDialog dialog = new SpectralUnmixingDialog(new AppContext() {
            private PropertyMap preferences = new PropertyMap();

            public void addProduct(Product product) {
                System.out.println("product added: " + product);
            }

            public Product[] getProducts() {
                return new Product[]{inputProduct};
            }

            public Product getSelectedProduct() {
                return inputProduct;
            }

            public Window getApplicationWindow() {
                return null;
            }

            public String getApplicationName() {
                return "Killer App";
            }

            public void handleError(Throwable e) {
                JOptionPane.showMessageDialog(getApplicationWindow(), e.getMessage());
            }

            public PropertyMap getPreferences() {
                return preferences;
            }
        });
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }


}
