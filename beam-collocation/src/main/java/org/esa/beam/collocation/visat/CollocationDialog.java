package org.esa.beam.collocation.visat;

import com.jidesoft.dialog.JideOptionPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JOptionPane;
import java.awt.Window;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationDialog extends ModalDialog {

    private CollocationFormModel formModel;
    private CollocationForm form;

    public CollocationDialog(Window parent, Product[] products) {
        super(parent, "Geographic Collocation", ModalDialog.ID_OK_CANCEL_HELP, "collocation");

        formModel = new CollocationFormModel(products);
        form = new CollocationForm(formModel);
    }

    @Override
    public int show() {
        setContent(form);
//        form.outputProductName.requestFocus();
        return super.show();
    }

    @Override
    protected void onOK() {
        JOptionPane.showMessageDialog(form, "Not implemented yet.", "Geographic Collocation",
                                      JideOptionPane.INFORMATION_MESSAGE);
        /* prototype code
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
        */
        super.onOK();
        /*
        VisatApp.getApp().addProduct(targetProduct);
        */
    }
}
