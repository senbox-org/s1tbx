/*
 * $Id: ShowImageViewRGBAction.java,v 1.1 2006/11/15 16:21:49 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.ui.RGBImageProfilePane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.Icon;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.io.IOException;

/**
 * This action opens an RGB image view on the currently selected Product.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ShowImageViewRGBAction extends ExecCommand {
    public static String ID = "showImageViewRGB";

    @Override
    public void actionPerformed(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product != null) {
            openProductSceneViewRGB(product, getHelpId());
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null);
    }

    public void openProductSceneViewRGB(final Product product, final String helpId) {
        final VisatApp visatApp = VisatApp.getApp();
        final RGBImageProfilePane profilePane = new RGBImageProfilePane(visatApp.getPreferences(), product);
        final boolean ok = profilePane.showDialog(visatApp.getMainFrame(), visatApp.getAppName() + " - Select RGB-Image Channels",
                                                  helpId);
        if (!ok) {
            return;
        }
        final String[] rgbaExpressions = profilePane.getRgbaExpressions();
        if (profilePane.getStoreProfileInProduct()) {
            RGBImageProfile.storeRgbaExpressions(product, rgbaExpressions);
        }

        final RGBImageProfile selectedProfile = profilePane.getSelectedProfile();
        final String name = selectedProfile != null ? selectedProfile.getName().replace("_", " ") : "";

        openProductSceneViewRGB(name + " RGB", product, rgbaExpressions, helpId);
    }

    /**
     * Creates product scene view using the given RGBA expressions.
     */
    public void openProductSceneViewRGB(final String name, final Product product, final String[] rgbaExpressions,
                                        final String helpId) {
        final VisatApp visatApp = VisatApp.getApp();
        final SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                visatApp.getMainFrame(),
                visatApp.getAppName() + " - Creating image for '" + name + "'") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                return createProductSceneImageRGB(name, product, rgbaExpressions, pm);
            }

            @Override
            protected void done() {
                visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());

                final ProductSceneImage productSceneImage;
                try {
                    productSceneImage = get();
                } catch (OutOfMemoryError e) {
                    visatApp.showOutOfMemoryErrorDialog("The RGB image view could not be created."); /*I18N*/
                    return;
                } catch (Exception e) {
                    visatApp.handleUnknownException(e);
                    return;
                }
                visatApp.clearStatusBarMessage();

                ProductSceneView productSceneView = new ProductSceneView(productSceneImage);
                productSceneView.setCommandUIFactory(visatApp.getCommandUIFactory());
                productSceneView.setNoDataOverlayEnabled(false);
                productSceneView.setROIOverlayEnabled(false);
                productSceneView.setGraticuleOverlayEnabled(false);
                productSceneView.setPinOverlayEnabled(false);
                productSceneView.setLayerProperties(visatApp.getPreferences());
                final String title = createInternalFrameTitle(product, productSceneImage.getName());
                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
                visatApp.createInternalFrame(title, icon, productSceneView, helpId);
                visatApp.addPropertyMapChangeListener(productSceneView);
                updateState();
            }
        };
        visatApp.setStatusBarMessage("Creating RGB image view...");  /*I18N*/
        visatApp.getExecutorService().submit(worker);
    }



    private static class RGBBand {

        private Band band;
        private boolean dataLoaded;
    }

    private ProductSceneImage createProductSceneImageRGB(String name, final Product product, String[] rgbaExpressions,
                                                         ProgressMonitor pm) throws Exception {
        final VisatApp visatApp = VisatApp.getApp();
        UIUtils.setRootFrameWaitCursor(visatApp.getMainFrame());
        RGBBand[] rgbBands = null;
        boolean errorOccured = false;
        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating RGB image...", 2);
            rgbBands = allocateRgbBands(product, rgbaExpressions);
            productSceneImage = new ProductSceneImage(name, rgbBands[0].band,
                                                         rgbBands[1].band,
                                                         rgbBands[2].band,
                                                         visatApp.getPreferences(),
                                                         SubProgressMonitor.create(pm, 1));
        } catch (Exception e) {
            errorOccured = true;
            throw e;
        } finally {
            pm.done();
            if (rgbBands != null) {
                releaseRgbBands(rgbBands, errorOccured);
            }
        }
        return productSceneImage;
    }

    private  RGBBand[] allocateRgbBands(final Product product,
                                        final String[] rgbaExpressions) throws IOException {
        final RGBBand[] rgbBands = new RGBBand[3]; // todo - set to [4] as soon as we support alpha
        for (int i = 0; i < rgbBands.length; i++) {
            final RGBBand rgbBand = new RGBBand();
            String expression = rgbaExpressions[i].isEmpty() ? "0" : rgbaExpressions[i];
            rgbBand.band = product.getBand(expression);
            if (rgbBand.band == null) {
                rgbBand.band = new ProductSceneView.RGBChannel(product,
                                                               RGBImageProfile.RGB_BAND_NAMES[i],
                                                               expression);
            }
            rgbBands[i] = rgbBand;
        }
        return rgbBands;
    }

    private static void releaseRgbBands(RGBBand[] rgbBands, boolean errorOccured) {
        for (int i = 0; i < rgbBands.length; i++) {
            final RGBBand rgbBand = rgbBands[i];
            if (rgbBand != null && rgbBand.band != null) {
                if (rgbBand.band instanceof ProductSceneView.RGBChannel) {
                    if (rgbBand.dataLoaded) {
                        rgbBand.band.unloadRasterData();
                    }
                    if (errorOccured) {
                        rgbBand.band.dispose();
                    }
                }
                rgbBand.band = null;
            }
            rgbBands[i] = null;
        }
    }

    private String createInternalFrameTitle(final Product product, String name) {
        return UIUtils.getUniqueFrameTitle(VisatApp.getApp().getAllInternalFrames(),
                                           product.getProductRefString() + " " + name);
    }

}
