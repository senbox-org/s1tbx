package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class ShowShapeOverlayAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            productSceneView.setShapeOverlayEnabled(isSelected());
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            setSelected(productSceneView.isShapeOverlayEnabled());
        }
        setEnabled(productSceneView != null && productSceneView.getCurrentShapeFigure() != null);
    }
}
