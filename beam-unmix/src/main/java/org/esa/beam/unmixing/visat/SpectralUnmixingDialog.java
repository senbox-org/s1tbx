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
package org.esa.beam.unmixing.visat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Guardian;
import org.esa.beam.visat.VisatApp;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import java.awt.Window;


public class SpectralUnmixingDialog extends ModalDialog {
    SpectralUnmixingForm form;
    SpectralUnmixingFormModel formModel;
    private InternalFrameAdapter internalFrameAdapter;

    public SpectralUnmixingDialog(final Window parent, final Product inputProduct) {
        super(parent, "Spectral Unmixing", ModalDialog.ID_OK_CANCEL_HELP, "spectralUnmixing");
        Guardian.assertNotNull("inputProduct", inputProduct);
        formModel = new SpectralUnmixingFormModel(inputProduct);
        form = new SpectralUnmixingForm(formModel);
    }

    @Override
    public int show() {
        setContent(form);
        form.outputProductName.requestFocus();
        return super.show();
    }


    @Override
    protected void onOK() {
        formModel.getOperatorParameters().put("endmembers", form.getEndmemberPresenter().getEndmembers());

        Product outputProduct;
        try {
            outputProduct = GPF.createProduct("SpectralUnmixing",
                                              formModel.getOperatorParameters(),
                                              formModel.getInputProduct());
        } catch (OperatorException e) {
            showErrorDialog(e.getMessage());
            return;
        }
        super.onOK();
        if (outputProduct != formModel.getInputProduct()) {
            outputProduct.setName(form.outputProductName.getText());
            VisatApp.getApp().addProduct(outputProduct);
        }
    }

    @Override
    protected boolean verifyUserInput() {
        // todo - verify sourceBandNames & endmembers
        return true;
    }

    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Product inputProduct = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < 15; i++) {
            Band band = inputProduct.addBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32);
            band.setSpectralWavelength(500 + i * 30);
            band.setSpectralBandIndex(i);
        }
        inputProduct.addBand("l1_flags", ProductData.TYPE_UINT32);

        SpectralUnmixingDialog dialog = new SpectralUnmixingDialog(null, inputProduct);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }
}
