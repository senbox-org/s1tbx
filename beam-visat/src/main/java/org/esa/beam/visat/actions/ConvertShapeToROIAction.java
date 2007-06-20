package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.roi.RoiManagerToolView;

import javax.swing.JOptionPane;

public class ConvertShapeToROIAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            convertShapeToROI(productSceneView);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(productSceneView != null && productSceneView.getCurrentShapeFigure() != null);
    }

    private static void convertShapeToROI(ProductSceneView productSceneView) {
        Figure roiShapeFigure = productSceneView.getCurrentShapeFigure();
        if (roiShapeFigure == null) {
            return;  // Should not come here...
        }

        ROIDefinition roiDefinition = productSceneView.getRaster().getROIDefinition();
        if (roiDefinition != null) {
            roiDefinition = roiDefinition.createCopy(); // we need a new instance in order to force node change
        } else {
            roiDefinition = new ROIDefinition();
            roiDefinition.setValueRangeEnabled(false);
            roiDefinition.setBitmaskEnabled(false);
            roiDefinition.setPinUseEnabled(false);
        }
        roiDefinition.setShapeEnabled(true);
        roiDefinition.setShapeFigure(roiShapeFigure);

        productSceneView.setROIOverlayEnabled(true);
        productSceneView.getRaster().setROIDefinition(roiDefinition);
        VisatApp visatApp = VisatApp.getApp();
        final int status = visatApp.showQuestionDialog("Shape To ROI",
                                                       "The shape has been been successfully converted to a ROI.\n" +
                                                       "Do you wish to delete the shape now?",
                                                       false, null); /*I18N*/
        if (status == JOptionPane.YES_OPTION) {
            productSceneView.setCurrentShapeFigure(null);
        }
        RoiManagerToolView roiDefinitionWindow = (RoiManagerToolView) visatApp.getPage().getToolView(
                RoiManagerToolView.ID);
        if (roiDefinitionWindow != null) {
            roiDefinitionWindow.setUIParameterValues(roiDefinition);
        }
        visatApp.updateState();
    }
}
