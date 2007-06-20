package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.roi.RoiManagerToolView;

import javax.swing.JOptionPane;

public class ConvertROIToShapeAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            convertROIToShape(productSceneView);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        event.getSelectableCommand().setEnabled(
                productSceneView != null &&
                productSceneView.getRasterROIShapeFigure() != null &&
                productSceneView.getRasterROIShapeFigure() != productSceneView.getCurrentShapeFigure());
    }

    private static void convertROIToShape(ProductSceneView productSceneView) {
        ROIDefinition roiDefinition = productSceneView.getRaster().getROIDefinition();
        if (roiDefinition == null) {
            return;  // Should not come here...
        }

        Figure currentShapeFigure = productSceneView.getCurrentShapeFigure();
        if (currentShapeFigure != null) {
            final int status = VisatApp.getApp().showQuestionDialog("ROI to Shape", /*I18N*/
                                                                    "The current shape will be replaced by the ROI shape.\n" +
                                                                    "Do you wish to continue?", /*I18N*/
                                                                                                false, null);
            if (status == JOptionPane.NO_OPTION) {
                return;
            }
        }

        final Figure newShapeFigure = productSceneView.getRasterROIShapeFigure();
        productSceneView.setCurrentShapeFigure(newShapeFigure);
        productSceneView.setShapeOverlayEnabled(true);
        final int status = VisatApp.getApp().showQuestionDialog("ROI to Shape", /*I18N*/
                                                                "The ROI has been successfully converted to a shape.\n" +
                                                                "Do you also wish to exclude the shape from the current ROI?",
                                                                /*I18N*/
                                                                false, null);
        if (status == JOptionPane.YES_OPTION) {
            roiDefinition.setShapeEnabled(false);

            RoiManagerToolView roiDefinitionWindow = (RoiManagerToolView) VisatApp.getApp().getPage().getToolView(
                    RoiManagerToolView.ID);
            if (roiDefinitionWindow != null) {
                roiDefinitionWindow.setUIParameterValues(roiDefinition);
            }

            VisatApp.getApp().updateROIImage(productSceneView, true);
        }
        VisatApp.getApp().updateState();
    }
}
