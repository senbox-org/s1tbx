/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.text.MessageFormat;

/**
 * This action opens an image view of the currently selected raster.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ShowImageViewAction extends ExecCommand {

    public static final String ID = "showImageView";

    @Override
    public void actionPerformed(final CommandEvent event) {
        final VisatApp visatApp = VisatApp.getApp();
        openProductSceneView((RasterDataNode) visatApp.getSelectedProductNode());
    }

    public void openProductSceneView(final RasterDataNode selectedProductNode) {
        VisatApp.getApp().setStatusBarMessage("Opening image view...");
        UIUtils.setRootFrameWaitCursor(VisatApp.getApp().getMainFrame());

        String progressMonitorTitle = MessageFormat.format("{0} - Creating image for ''{1}''",
                                                           VisatApp.getApp().getAppName(),
                                                           selectedProductNode.getName());

        SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(VisatApp.getApp().getMainFrame(),
                                                                                       progressMonitorTitle) {

            @Override
            protected ProductSceneImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
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
                UIUtils.setRootFrameDefaultCursor(VisatApp.getApp().getMainFrame());
                VisatApp.getApp().clearStatusBarMessage();
                try {
                    ProductSceneView view = new ProductSceneView(get());
                    openInternalFrame(view);
                } catch (OutOfMemoryError ignored) {
                    VisatApp.getApp().showOutOfMemoryErrorDialog("Failed to open image view.");
                } catch (Exception e) {
                    VisatApp.getApp().handleError(
                            MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
                }
            }
        };
        worker.execute();
    }

    public JInternalFrame openInternalFrame(final ProductSceneView view) {
        return openInternalFrame(view, true);
    }

    public JInternalFrame openInternalFrame(final ProductSceneView view, boolean configureByPreferences) {
        final VisatApp visatApp = VisatApp.getApp();
        final RasterDataNode selectedProductNode = view.getRaster();
        view.setCommandUIFactory(visatApp.getCommandUIFactory());
        if (configureByPreferences) {
            view.setLayerProperties(visatApp.getPreferences());
        }

        final String title = createInternalFrameTitle(selectedProductNode);
        final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
        final JInternalFrame internalFrame = visatApp.createInternalFrame(title, icon, view, getHelpId(), true);
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
        product.addProductNodeListener(pnl);
        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent event11) {
                product.removeProductNodeListener(pnl);
            }
        });

        visatApp.updateState();

        return internalFrame;
    }

    private String createInternalFrameTitle(final RasterDataNode raster) {
        return UIUtils.getUniqueFrameTitle(VisatApp.getApp().getAllInternalFrames(), raster.getDisplayName());
    }

    protected ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);
            JInternalFrame[] frames = VisatApp.getApp().findInternalFrames(raster, 1);
            ProductSceneImage sceneImage;
            if (frames.length > 0) {
                ProductSceneView view = (ProductSceneView) frames[0].getContentPane();
                sceneImage = new ProductSceneImage(raster, view);
            } else {
                sceneImage = new ProductSceneImage(raster,
                                                   VisatApp.getApp().getPreferences(),
                                                   SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
        }

    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProductNode() instanceof RasterDataNode);
    }
}
