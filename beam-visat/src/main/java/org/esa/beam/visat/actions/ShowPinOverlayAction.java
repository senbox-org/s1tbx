package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;

public class ShowPinOverlayAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView == null) {
            return;
        }
        Product product = productSceneView.getProduct();
        if (ProductUtils.canGetPixelPos(product)) {
            productSceneView.setPinOverlayEnabled(isSelected());
            VisatApp.getApp().updateState();
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (productSceneView != null) {
            setSelected(productSceneView.isPinOverlayEnabled());
            Product product = productSceneView.getProduct();
            if (ProductUtils.canGetPixelPos(product)) {
                enabled = true;
            }
        }
        setEnabled(enabled);
    }
}
