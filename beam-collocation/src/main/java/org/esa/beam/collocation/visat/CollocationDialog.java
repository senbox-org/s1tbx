package org.esa.beam.collocation.visat;

import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

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
        super(parent, "Geographic Collocation", ID_OK_CANCEL_HELP, "collocation");

        formModel = new CollocationFormModel();
        form = new CollocationForm(formModel, products);
    }

    @Override
    public int show() {
        setContent(form);
//        form.outputProductName.requestFocus();
        return super.show();
    }

    @Override
    protected void onOK() {
        // todo - dispose form

        final Product targetProduct;

        try {
            final Map<String, Product> productMap = new HashMap<String, Product>(5);
            productMap.put("master", formModel.getMasterProduct());
            productMap.put("slave", formModel.getSlaveProduct());

            final Map<String, Object> parameterMap = new HashMap<String, Object>(5);
            // collocation parameters
            parameterMap.put("targetProductName", formModel.getTargetProductName());
            parameterMap.put("renameMasterComponents", formModel.isRenameMasterComponentsSelected());
            parameterMap.put("renameSlaveComponents", formModel.isRenameSlaveComponentsSelected());
            parameterMap.put("masterComponentPattern", formModel.getMasterComponentPattern());
            parameterMap.put("slaveComponentPattern", formModel.getSlaveComponentPattern());
            parameterMap.put("resampling", formModel.getResampling());

            targetProduct = GPF.createProduct("Collocate", parameterMap, productMap);
            targetProduct.setName(formModel.getTargetProductName());

            if (formModel.isSaveToFileSelected()) {
                DialogProgressMonitor pm = new DialogProgressMonitor(getJDialog(), "Writing product...",
                        Dialog.ModalityType.APPLICATION_MODAL);
                try {
                    WriteOp.writeProduct(targetProduct,
                            formModel.getTargetFile(),
                            formModel.getTargetFormatName(), pm);
                } catch (IOException e) {
                    throw new OperatorException(MessageFormat.format(
                            "Could not write product to file ''{0}''", formModel.getTargetFilePath()), e);
                }
            }
        } catch (OperatorException e) {
            showErrorDialog(e.getMessage());
            return;
        }

        super.onOK();

        if (formModel.isOpenInVisatSelected()) {
            VisatApp.getApp().addProduct(targetProduct);
        }
    }
}
