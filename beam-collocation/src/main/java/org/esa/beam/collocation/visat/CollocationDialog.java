package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationDialog extends SingleTargetProductDialog {

    private CollocationFormModel formModel;
    private CollocationForm form;

    public CollocationDialog(AppContext appContext) {
        super(appContext, "Collocation", "collocation");
        formModel = new CollocationFormModel(getTargetProductSelector().getModel());
        form = new CollocationForm(formModel, getTargetProductSelector(), appContext);
    }

    @Override
    protected Product createTargetProduct() throws Exception {
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
        parameterMap.put("resamplingType", formModel.getResamplingType());

        return GPF.createProduct("Collocate", parameterMap, productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }
}
