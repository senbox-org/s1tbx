package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

public class ShowROIOverlayAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            productSceneView.setROIOverlayEnabled(isSelected());
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            setSelected(productSceneView.isROIOverlayEnabled());
        }
        setEnabled(productSceneView != null);
    }
}
