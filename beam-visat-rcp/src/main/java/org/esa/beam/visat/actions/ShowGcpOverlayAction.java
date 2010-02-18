package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class ShowGcpOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.setGcpOverlayEnabled(isSelected());
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(view.getProduct().getGcpGroup().getNodeCount() > 0);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        setSelected(view.isGcpOverlayEnabled());
    }
}
