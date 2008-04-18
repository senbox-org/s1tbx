package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 14.11.2006
 * Time: 15:59:53
 * To change this template use File | Settings | File Templates.
 */
public class ShowGraticuleOverlayAction extends ExecCommand {

    public ShowGraticuleOverlayAction() {
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            RasterDataNode raster = productSceneView.getRaster();
            if (ProductUtils.canGetPixelPos(raster)) {
                productSceneView.setGraticuleOverlayEnabled(event.getSelectableCommand().isSelected());
            }
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (productSceneView != null) {
            event.getSelectableCommand().setSelected(productSceneView.isGraticuleOverlayEnabled());
            RasterDataNode raster = productSceneView.getRaster();
            if (ProductUtils.canGetPixelPos(raster)) {
                enabled = true;
            }
        }
        event.getSelectableCommand().setEnabled(enabled);
    }
}
