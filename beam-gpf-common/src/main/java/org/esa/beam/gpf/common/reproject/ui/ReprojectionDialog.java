package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.gpf.common.reproject.ReprojectionOp;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionDialog extends SingleTargetProductDialog {
    private ReprojectionFormModel formModel;
    private ReprojectionForm form;

    public ReprojectionDialog(AppContext appContext) throws FactoryException {
        super(appContext, "Reproject", "reproject");
        formModel = new ReprojectionFormModel();
        form = new ReprojectionForm(formModel, getTargetProductSelector(), appContext);
    }

    @Override
    protected Product createTargetProduct() throws Exception {

        final ProjectedCRS crs = formModel.getTargetCrs();
        String interpolationName = formModel.getInterpolationName();
        
        if (crs != null) {
            ReprojectionOp op = ReprojectionOp.create(formModel.getSourceProduct(), crs, interpolationName);
            return op.getTargetProduct();
        } else {
            final Map<String, Product> productMap = new HashMap<String, Product>(5);
            productMap.put("source", formModel.getSourceProduct());
            final Map<String, Object> parameterMap = new HashMap<String, Object>(5);
            // Reprojection parameters
            parameterMap.put("interpolationName", interpolationName);
            return GPF.createProduct("Reproject", parameterMap, productMap);
        }
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
