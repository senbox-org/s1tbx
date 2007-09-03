package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class ShowGcpOverlayAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            productSceneView.setGcpOverlayEnabled(isSelected());
            VisatApp.getApp().updateState();
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (productSceneView != null) {
            setSelected(productSceneView.isGcpOverlayEnabled());
            enabled = true;
        }
        setEnabled(enabled);
    }
}
