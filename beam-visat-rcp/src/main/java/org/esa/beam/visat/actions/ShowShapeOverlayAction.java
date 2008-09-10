package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class ShowShapeOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            view.setShapeOverlayEnabled(isSelected());
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(view != null && view.getCurrentShapeFigure() != null);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        setSelected(view != null && view.isShapeOverlayEnabled());
    }
}
