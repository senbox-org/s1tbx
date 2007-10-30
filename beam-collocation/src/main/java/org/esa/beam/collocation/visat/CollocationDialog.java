package org.esa.beam.collocation.visat;

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;

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
        return super.show();
    }

    @Override
    protected void onOK() {
        super.onOK();

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
                final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker<Boolean, Boolean>(getJDialog(), "Writing product") {

                    @Override
                    protected Boolean doInBackground(ProgressMonitor pm) throws Exception {
                        System.out.println("saving");
                        
                        WriteOp.writeProduct(targetProduct,
                                formModel.getTargetFile(),
                                formModel.getTargetFormatName(), pm);
                        return true;
                    }
                
                };
                worker.executeWithBlocking();
                targetProduct.dispose();
                if (formModel.isOpenInVisatSelected()) {
                    Product openProduct;
                    openProduct = ProductIO.readProduct(formModel.getTargetFile(), null);
                    VisatApp.getApp().addProduct(openProduct);
                }
            } else {
                if (formModel.isOpenInVisatSelected()) {
                    VisatApp.getApp().addProduct(targetProduct);
                }
            }
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        } finally {
            dispose();
        }
    }
    
    @Override
    protected void onCancel() {
        super.onCancel();
        dispose();
    }

    private void dispose() {
        form.dispose();
    }
}
