package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.ProductFlipDialog;

public class FlippingAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        openFlippingDialog(VisatApp.getApp(), getHelpId());
    }

    @Override
    public void updateState(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getNumBands() + product.getNumTiePointGrids() > 0);
    }

    private static void openFlippingDialog(final VisatApp visatApp, final String helpId) {

        final Product partentProduct = visatApp.getSelectedProduct();
        final ProductFlipDialog dialog = new ProductFlipDialog(visatApp.getMainFrame(), partentProduct);
        if (dialog.show() == ModalDialog.ID_OK) {
            final Product product = dialog.getResultProduct();
            if (product != null) {
                visatApp.addProduct(product);
            } else if (dialog.getException() != null) {
                visatApp.showErrorDialog("The flipped product could not be created:\n" +
                                         dialog.getException().getMessage());
            }
        }
    }

}
