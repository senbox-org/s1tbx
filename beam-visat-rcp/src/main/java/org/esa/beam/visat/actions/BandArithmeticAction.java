package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.BandArithmetikDialog;

import javax.swing.SwingUtilities;

/**
 * VISAT's band arithmetic feature.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class BandArithmeticAction extends ExecCommand {

    private BandArithmetikDialog _bandArithmetikDialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        openBandArithmeticDialog(VisatApp.getApp(), event.getCommand().getHelpId());
    }

    @Override
    public void updateState(final CommandEvent event) {
        final int n = VisatApp.getApp().getProductManager().getNumProducts();
        setEnabled(n > 0);
    }

    @Override
    public void updateComponentTreeUI() {
        if (_bandArithmetikDialog != null) {
            SwingUtilities.updateComponentTreeUI(_bandArithmetikDialog.getJDialog());
        }
    }

    private void openBandArithmeticDialog(final VisatApp visatApp, final String helpId) {

        final Product[] prods = visatApp.getProductManager().getProducts();
        final ProductNodeList<Product> products = new ProductNodeList<Product>();
        for (Product prod : prods) {
            products.add(prod);
        }
        if (_bandArithmetikDialog == null) {
            _bandArithmetikDialog = new BandArithmetikDialog(visatApp,
                                                             visatApp.getSelectedProduct(),
                                                             products,
                                                             helpId);
        } else {
            _bandArithmetikDialog.setTargetProduct(visatApp.getSelectedProduct(), products);
        }
        if (_bandArithmetikDialog.show() == ModalDialog.ID_OK) {
            final Product product = _bandArithmetikDialog.getTargetProduct();
            if (!products.contains(product)) {
                visatApp.addProduct(product);
            } else {
//                visatApp.getProductTree().updateProductNodes(product);
            }
//            visatApp.setSelectedProductNode(product.getBandAt(product.getNumBands() - 1));
        }
    }
}
