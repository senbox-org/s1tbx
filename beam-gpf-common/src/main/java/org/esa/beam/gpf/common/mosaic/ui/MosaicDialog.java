package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import java.util.Map;

import com.bc.ceres.binding.PropertyContainer;

/**
 * User: Marco
 * Date: 16.08.2009
 */
class MosaicDialog extends SingleTargetProductDialog {

    private final MosaicForm form;

    public static void main(String[] args) {
        final DefaultAppContext context = new DefaultAppContext("Mosaic");
        final MosaicDialog dialog = new MosaicDialog("MosaicTestDialog", null, context);
        dialog.show();

    }

    MosaicDialog(final String title, final String helpID, AppContext appContext) {
        super(appContext, title, helpID);
        final TargetProductSelector selector = getTargetProductSelector();
        selector.getModel().setSaveToFileSelected(true);
        selector.getSaveToFileCheckBox().setEnabled(false);
        form = new MosaicForm(selector, appContext);
    }

    @Override
    protected boolean verifyUserInput() {
        // todo
        return true;
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final MosaicFormModel formModel = form.getFormModel();
        return GPF.createProduct("Mosaic", formModel.getParameterMap(), formModel.getSourceProductMap());
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