package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.visat.VisatApp;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;

public class StatisticDialogHelper {
    public static void openStatisticsDialog(final int tabIndex) {
        StatisticsToolView statisticsToolView = (StatisticsToolView) VisatApp.getApp().getPage().getToolView(StatisticsToolView.ID);
        statisticsToolView.show(tabIndex);
    }

    public static RasterDataNode getSelectedRasterDataNode(final VisatApp visatApp) {
       final ProductNode selectedProductNode = visatApp.getSelectedProductNode();
       return selectedProductNode instanceof RasterDataNode ? (RasterDataNode) selectedProductNode : null;
    }

    public static void enableCommandIfProductSelected(final VisatApp visatApp, final CommandEvent event) {
       final Product product = visatApp.getSelectedProduct();
       event.getSelectableCommand().setEnabled(product != null);
    }

    public static void enableCommandIfRasterSelected(final VisatApp visatApp, final CommandEvent event) {
       final RasterDataNode raster = getSelectedRasterDataNode(visatApp);
       event.getSelectableCommand().setEnabled(raster != null);
    }

    public static void enableCommandIfShapeSelected(final VisatApp visatApp, final CommandEvent event) {
       final ProductSceneView view = visatApp.getSelectedProductSceneView();
       event.getSelectableCommand().setEnabled(view != null && view.getCurrentShapeFigure() != null);
    }
}
