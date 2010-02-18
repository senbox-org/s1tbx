package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.BandMathsDialog;


/**
 * VISAT's band arithmetic feature.
 *
 * @author Norman Fomferra
 * @version $Revision: 8410 $ $Date: 2010-02-14 14:31:41 +0100 (So, 14 Feb 2010) $
 */
public class BandMathsAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        openBandArithmeticDialog(VisatApp.getApp(), event.getCommand().getHelpId());
    }

    @Override
    public void updateState(final CommandEvent event) {
        final int n = VisatApp.getApp().getProductManager().getProductCount();
        setEnabled(n > 0 && VisatApp.getApp().getSelectedProduct() != null); 
    }

   
    private void openBandArithmeticDialog(final VisatApp visatApp, final String helpId) {

        final Product[] prods = visatApp.getProductManager().getProducts();
        final ProductNodeList<Product> products = new ProductNodeList<Product>();
        for (Product prod : prods) {
            products.add(prod);
        }
        BandMathsDialog bandMathsDialog = new BandMathsDialog(visatApp,
                                                         visatApp.getSelectedProduct(),
                                                         products,
                                                         helpId);
        bandMathsDialog.show();
    }
}
