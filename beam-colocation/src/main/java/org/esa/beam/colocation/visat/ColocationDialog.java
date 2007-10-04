package org.esa.beam.colocation.visat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import java.awt.Dialog;
import java.awt.Window;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class ColocationDialog extends ModalDialog {

    private ColocationFormModel formModel;
    private ColocationForm form;

    public ColocationDialog(Window parent, Product selectedProduct) {
        super(parent, "Geographic Colocation", ModalDialog.ID_OK_CANCEL_HELP, "colocation");

        formModel = new ColocationFormModel(selectedProduct);
        form = new ColocationForm(formModel);
    }

    @Override
    public int show() {
        setContent(form);
//        form.outputProductName.requestFocus();
        return super.show();
    }

    @Override
    protected void onOK() {
        final Product targetProduct;
        try {
            DialogProgressMonitor pm = new DialogProgressMonitor(getJDialog(), "Geographic Collocation",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);
            final HashMap<String, Product> productMap = new HashMap<String, Product>(5);

            final Product masterProduct = VisatApp.getApp().getProductManager().getProductAt(0);
            final Product slaveProduct = VisatApp.getApp().getProductManager().getProductAt(1);

            productMap.put("master", masterProduct);
            productMap.put("slave", slaveProduct);

            targetProduct = GPF.createProduct("Collocation", new HashMap<String, Object>(0), productMap,
                                              ProgressMonitor.NULL);
        } catch (OperatorException e) {
            showErrorDialog(e.getMessage());
            return;
        }
        super.onOK();
        VisatApp.getApp().addProduct(targetProduct);
    }

}
