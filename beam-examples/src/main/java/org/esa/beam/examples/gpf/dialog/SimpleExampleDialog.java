package org.esa.beam.examples.gpf.dialog;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

/**
 * Date: 13.07.11
 */
public class SimpleExampleDialog extends SingleTargetProductDialog {

    private String alias;
    private OperatorParameterSupport parameterSupport;
    private SimpleForm form;

    public SimpleExampleDialog(String alias, AppContext appContext, String title, String helpId) {
        super(appContext, title, helpId);

        this.alias = alias;
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(alias);

        parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorClass());
        form = new SimpleForm(appContext, operatorSpi, parameterSupport.getPopertySet(),
                              getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorClass(),
                                                     parameterSupport,
                                                     helpId);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        return GPF.createProduct(alias, parameterSupport.getParameterMap(), sourceProduct);
    }

    @Override
    protected void onApply() {
        if (validateUserInput()) {
            super.onApply();
        }

    }

    private boolean validateUserInput() {
        return true;
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
