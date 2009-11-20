package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
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
        final TargetProductSelector selector = getTargetProductSelector();
        selector.getModel().setSaveToFileSelected(true);
        selector.getSaveToFileCheckBox().setEnabled(false);
        form = new MosaicForm(selector, appContext);
    }

    @Override
    protected boolean verifyUserInput() {
        final MosaicFormModel formModel = form.getFormModel();
        if (!verifySourceProducts(formModel)) {
            return false;
        }
        if (!verfiyTargetCrs(formModel)) {
            return false;
        }
        if(formModel.isUpdateMode() && formModel.getUpdateProduct() == null) {
            showErrorDialog("No product to update specified.");
            return false;
        }
        final boolean varsNotSpecified = formModel.getVariables() == null || formModel.getVariables().length == 0;
        final boolean condsNotSpecified = formModel.getConditions() == null || formModel.getConditions().length == 0;
        if(varsNotSpecified && condsNotSpecified) {
            showErrorDialog("No variables or conditions specified.");
            return false;
        }

        return true;
    }

    private boolean verfiyTargetCrs(MosaicFormModel formModel) {
        try {
            final CoordinateReferenceSystem crs = formModel.getTargetCRS();
            if(crs == null) {
                showErrorDialog("No 'Coordinate Reference System' selected.");
                return false;
            }
        } catch (FactoryException e) {
            e.printStackTrace();
            showErrorDialog("No 'Coordinate Reference System' selected.\n" + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean verifySourceProducts(MosaicFormModel formModel) {
        try {
            final Map<String, Product> sourceProductMap = formModel.getSourceProductMap();
            if(sourceProductMap == null || sourceProductMap.isEmpty()) {
                showErrorDialog("No source products specified.");
                return false;
            }
        } catch (IOException e) {
            showErrorDialog("Error while reading source product.\n" + e.getMessage());
            return false;
        }
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