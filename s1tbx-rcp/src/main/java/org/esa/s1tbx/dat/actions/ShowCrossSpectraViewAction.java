/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.s1tbx.dat.views.polarview.PolarView;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.ui.UIUtils;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.framework.ui.product.ProductSceneImage;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.Debug;

import javax.swing.*;

/**
 * This action opens a polar wave view for the currently selected wave product.
 */
public class ShowCrossSpectraViewAction extends ExecCommand {
    public static String ID = "showPolarWaveView";

    @Override
    public void actionPerformed(final CommandEvent event) {
        openProductSceneView((RasterDataNode) SnapApp.getDefault().getSelectedProductNode());
    }

    void openProductSceneView(final RasterDataNode selectedProductNode) {
        SnapApp.getDefault().setStatusBarMessage("Creating polar view...");
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        final Product product = SnapApp.getDefault().getSelectedProduct();

        final SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(SnapApp.getDefault().getMainFrame(),
                 SnapApp.getDefault().getAppName() + " - Creating image for '" + selectedProductNode.getName() + "'") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                try {
                    return createProductSceneImage(selectedProductNode, pm);
                } finally {
                    if (pm.isCanceled()) {
                        selectedProductNode.unloadRasterData();
                    }
                }
            }

            @Override
            public void done() {
                UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
                SnapApp.getDefault().setStatusBarMessage("");

                final ProductSceneImage productSceneImage;
                try {
                    productSceneImage = get();
                } catch (OutOfMemoryError e) {
                    SnapApp.getDefault().handleError("The polar view could not be created.", e);
                    return;
                } catch (Exception e) {
                    SnapApp.getDefault().handleError("The polar view could not be created.", e);
                    return;
                }

                final PolarView view = new PolarView(product, productSceneImage);
                //view.setCommandUIFactory(visatApp.getCommandUIFactory());
/* todo
                final String title = createInternalFrameTitle(selectedProductNode);
                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
                final JInternalFrame internalFrame = visatApp.createInternalFrame(title, icon, view, getHelpId());
                final ProductNodeListenerAdapter pnl = new ProductNodeListenerAdapter() {
                    @Override
                    public void nodeChanged(final ProductNodeEvent event1) {
                        if (event1.getSourceNode() == selectedProductNode &&
                                event1.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                            internalFrame.setTitle(createInternalFrameTitle(selectedProductNode));
                        }
                    }
                };
                final Product product = selectedProductNode.getProduct();
                internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
                    @Override
                    public void internalFrameOpened(InternalFrameEvent event1) {
                        product.addProductNodeListener(pnl);
                    }

                    @Override
                    public void internalFrameClosed(InternalFrameEvent event11) {
                        product.removeProductNodeListener(pnl);
                    }
                });

                //visatApp.updateState();
                */
            }
        };
       //todo visatApp.getExecutorService().submit(worker);
    }

    private static ProductSceneImage createProductSceneImage(final RasterDataNode raster,
                                                             ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        ProductSceneImage sceneImage = null;
        try {
            pm.beginTask("Creating polar view...", 1);
           /* final JInternalFrame[] frames = app.findInternalFrames(raster, 1);
            if (frames.length > 0) {
                final ProductSceneView view = (ProductSceneView) frames[0].getContentPane();
                sceneImage = new ProductSceneImage(raster, view);
            } else {
                sceneImage = new ProductSceneImage(raster, SnapApp.getDefault().getCompatiblePreferences(), SubProgressMonitor.create(pm, 1));
            }*/
        } finally {
            pm.done();
        }

        return sceneImage;
    }

    @Override
    public void updateState(final CommandEvent event) {
        final Product product = SnapApp.getDefault().getSelectedProduct();
        if (product != null) {
            final String productType = SnapApp.getDefault().getSelectedProduct().getProductType();
            setEnabled(productType.startsWith("ASA_WV") &&
                               SnapApp.getDefault().getSelectedProductNode() instanceof RasterDataNode);
        } else
            setEnabled(false);
    }
}
