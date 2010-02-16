package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.MapProjectionDialog;

public class ProjectionAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        openProjectionDialog(VisatApp.getApp(), getHelpId());
    }

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(canGetPixelPos(product));
    }

    private static boolean canGetPixelPos(Product product) {
        return product != null
               && product.getGeoCoding() != null
               && product.getGeoCoding().canGetPixelPos();
    }

    private static void openProjectionDialog(final VisatApp visatApp, String helpId) {

        final Product baseProduct = visatApp.getSelectedProduct();
        if (!canGetPixelPos(baseProduct)) {
            // should not come here...
            return;
        }

        final MapProjectionDialog dialog = new MapProjectionDialog(visatApp.getMainFrame(), baseProduct, false);
        if (helpId != null && helpId.length() > 0) {
            HelpSys.enableHelp(dialog.getJDialog(), helpId);
            HelpSys.enableHelpKey(dialog.getJDialog(), helpId);
        }

        if (dialog.show() == ModalDialog.ID_OK) {
            final Product product = dialog.getOutputProduct();
            if (product != null) {
                visatApp.addProduct(product);
            } else if (dialog.getException() != null) {
                visatApp.showErrorDialog("Map-projected product could not be created:\n" +
                                         dialog.getException().getMessage());                   /*I18N*/
            }
        }
    }
}
