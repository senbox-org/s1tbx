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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
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
import java.util.ArrayList;

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
        final VisatApp visatApp = VisatApp.getApp();
        visatApp.setStatusBarMessage("Creating image view...");
        UIUtils.setRootFrameWaitCursor(visatApp.getMainFrame());
        final ArrayList<String> expressionList = new ArrayList<String>();
        expressionList.add(selectedProductNode.getValidPixelExpression());
        if (selectedProductNode instanceof VirtualBand) {
            expressionList.add(((VirtualBand) selectedProductNode).getExpression());
        }
        for (String expression : expressionList) {
            if (expression != null) {
                final ProductManager productManager = visatApp.getProductManager();
                final int productIndex = productManager.getProductIndex(selectedProductNode.getProduct());
                final Product[] products = productManager.getProducts();
                try {
                    BandArithmetic.parseExpression(expression, products, productIndex);
                } catch (ParseException e) {
                    e.printStackTrace();
                    VisatApp.getApp().showErrorDialog(MessageFormat.format("Failed to create image view.\n " +
                                                                           "The expression ''{0}'' is invalid:\n\n{1}",
                                                                           expression, e.getMessage()));
                    return;
                }
            }
        }

        final SwingWorker worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(visatApp.getMainFrame(),
                                                                                             visatApp.getAppName() + " - Creating image for '" + selectedProductNode.getName() + "'") {

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
                UIUtils.setRootFrameDefaultCursor(visatApp.getMainFrame());
                visatApp.clearStatusBarMessage();

                try {
                    ProductSceneView view = new ProductSceneView(get());
                    openInternalFrame(view);
                } catch (OutOfMemoryError ignored) {
                    visatApp.showOutOfMemoryErrorDialog("The image view could not be created.");
                } catch (Exception e) {
                    visatApp.handleUnknownException(e);
                }
            }
        };
        visatApp.getExecutorService().submit(worker);
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

        visatApp.updateState();

        return internalFrame;
    }

    private String createInternalFrameTitle(final RasterDataNode raster) {
        return UIUtils.getUniqueFrameTitle(VisatApp.getApp().getAllInternalFrames(), raster.getDisplayName());
    }

    private ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);
        final VisatApp app = VisatApp.getApp();

        ProductSceneImage sceneImage = null;
        try {
            pm.beginTask("Creating image...", 1);
            final JInternalFrame[] frames = app.findInternalFrames(raster, 1);
            if (frames.length > 0) {
                final ProductSceneView view = (ProductSceneView) frames[0].getContentPane();
                sceneImage = new ProductSceneImage(raster, view);
            } else {
                sceneImage = new ProductSceneImage(raster, app.getPreferences(), SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
        } finally {
            pm.done();
        }

        return sceneImage;
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProductNode() instanceof RasterDataNode);
    }
}
