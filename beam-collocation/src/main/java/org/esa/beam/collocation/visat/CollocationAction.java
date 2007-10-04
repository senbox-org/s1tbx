package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationAction extends ExecCommand {

    @Override
    public void updateState(CommandEvent event) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        setEnabled(selectedProduct != null);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        if (selectedProduct != null) {
            final ModalDialog dialog = new CollocationDialog(VisatApp.getApp().getMainFrame(), selectedProduct);
            dialog.show();
        }
    }
}
