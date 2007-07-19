package org.esa.beam.unmixing.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameAdapter;


public class SpectralUnmixingAction extends ExecCommand {

    @Override
    public void updateState(CommandEvent commandEvent) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        setEnabled(selectedProduct != null);
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        if (selectedProduct != null) {
            SpectralUnmixingDialog dialog = new SpectralUnmixingDialog(VisatApp.getApp().getMainFrame(), selectedProduct);
            dialog.show();
        }
    }
}