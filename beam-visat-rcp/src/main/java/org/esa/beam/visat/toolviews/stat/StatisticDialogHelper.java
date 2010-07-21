/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
