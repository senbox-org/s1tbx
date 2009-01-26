package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class DeletePinAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView == null) {
            return;
        }
        final Product product = productSceneView.getProduct();
        if (product == null) {
            return;
        }
        final Pin selectedPin = product.getPinGroup().getSelectedNode();
        if (selectedPin == null) {
            return;
        }
        product.getPinGroup().remove(selectedPin);
        updateState();
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = productSceneView != null
                          && productSceneView.getProduct() != null
                          && productSceneView.getProduct().getPinGroup().getSelectedNode() != null;
        setEnabled(enabled);
    }
}
