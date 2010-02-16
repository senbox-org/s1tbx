package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;

public class ShowGraticuleOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            if (ProductUtils.canGetPixelPos(view.getRaster())) {
                view.setGraticuleOverlayEnabled(isSelected());
            }
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(ProductUtils.canGetPixelPos(view.getRaster()));
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        setSelected(view.isGraticuleOverlayEnabled());
    }
}
