package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.Map;

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
        form = new MosaicForm(getTargetProductSelector(), appContext);
    }

    @Override
    protected boolean verifyUserInput() {
        // todo
        return true;
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        // todo
//        final Map<String, Product> productMap = form.getProductMap();
//        final Map<String, Object> parameterMap = form.getParameterMap();
        return GPF.createProduct("Mosaic", null, (Map<String, Product>) null);
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