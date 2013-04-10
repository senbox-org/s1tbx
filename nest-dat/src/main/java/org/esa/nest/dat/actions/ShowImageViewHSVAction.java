/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ShowImageViewRGBAction;
import org.esa.nest.dat.dialogs.HSVImageProfilePane;

import javax.swing.*;
import java.awt.*;

/**
 * This action opens an HSV image view on the currently selected Product.
 *

 */
public class ShowImageViewHSVAction extends ExecCommand {

    public static String ID = "showImageViewHSV";

    private static final String r = "min(round( (floor((6*(h))%6)==0?(v): (floor((6*(h))%6)==1?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)): (floor((6*(h))%6)==2?((1-(s))*(v)): (floor((6*(h))%6)==3?((1-(s))*(v)): (floor((6*(h))%6)==4?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==5?(v):0)))))) *256), 255)";

    private static final String g = "min(round( (floor((6*(h))%6)==0?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==1?(v): (floor((6*(h))%6)==2?(v): (floor((6*(h))%6)==3?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)): (floor((6*(h))%6)==4?((1-(s))*(v)): (floor((6*(h))%6)==5?((1-(s))*(v)):0)))))) *256), 255)";

    private static final String b = "min(round( (floor((6*(h))%6)==0?((1-(s))*(v)): (floor((6*(h))%6)==1?((1-(s))*(v)): (floor((6*(h))%6)==2?((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v)): (floor((6*(h))%6)==3?(v): (floor((6*(h))%6)==4?(v): (floor((6*(h))%6)==5?((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v)):0)))))) *256), 255)";

    @Override
    public void actionPerformed(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product != null) {
            openProductSceneViewHSV(product, getHelpId());
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null);
    }

    public void openProductSceneViewHSV(final Product product, final String helpId) {
        final VisatApp visatApp = VisatApp.getApp();
        final Product[] openedProducts = visatApp.getProductManager().getProducts();
        final HSVImageProfilePane profilePane = new HSVImageProfilePane(visatApp.getPreferences(), product, openedProducts);
        final String title = "Select HSV-Image Channels";
        final boolean ok = profilePane.showDialog(visatApp.getMainFrame(), title, helpId);
        if (!ok) {
            return;
        }
        final String[] hsvExpressions = profilePane.getRgbaExpressions();
        nomalizeHSVExpressions(product, hsvExpressions);
        if (profilePane.getStoreProfileInProduct()) {
            RGBImageProfile.storeRgbaExpressions(product, hsvExpressions, HSVImageProfilePane.HSV_COMP_NAMES);
        }

        final String sceneName = createSceneName(product, profilePane.getSelectedProfile());
        openProductSceneViewHSV(sceneName, product, hsvExpressions);
    }

    /**
     * Creates product scene view using the given HSV expressions.
     */
    public void openProductSceneViewHSV(final String name, final Product product, final String[] hsvExpressions) {
        final VisatApp visatApp = VisatApp.getApp();
        final SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                visatApp.getMainFrame(),
                visatApp.getAppName() + " - Creating image for '" + name + '\'') {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {
                return createProductSceneImageHSV(name, product, hsvExpressions, pm);
            }

            @Override
            protected void done() {
                visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());

                try {
                    final ProductSceneView productSceneView = new ProductSceneView(get());
                    productSceneView.setLayerProperties(visatApp.getPreferences());
                    openInternalFrame(productSceneView, true);
                } catch (OutOfMemoryError e) {
                    visatApp.showOutOfMemoryErrorDialog("The HSV image view could not be created."); /*I18N*/
                    return;
                } catch (Exception e) {
                    visatApp.handleUnknownException(e);
                    return;
                }
                visatApp.clearStatusBarMessage();
            }
        };
        visatApp.setStatusBarMessage("Creating HSV image view...");  /*I18N*/
        visatApp.getExecutorService().submit(worker);
    }

    public JInternalFrame openInternalFrame(final ProductSceneView view, final boolean configureByPreferences) {
        final VisatApp visatApp = VisatApp.getApp();
        view.setCommandUIFactory(visatApp.getCommandUIFactory());
        if (configureByPreferences) {
            view.setLayerProperties(visatApp.getPreferences());
        }

        final String title = ShowImageViewRGBAction.createUniqueInternalFrameTitle(view.getSceneName());
        final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
        final JInternalFrame internalFrame = visatApp.createInternalFrame(title, icon, view, getHelpId());
        visatApp.addPropertyMapChangeListener(view);
        updateState();

        return internalFrame;
    }

    private static ProductSceneImage createProductSceneImageHSV(final String name, final Product product,
                                                         final String[] hsvExpressions,
                                                         final ProgressMonitor pm) throws Exception {
        final VisatApp visatApp = VisatApp.getApp();
        UIUtils.setRootFrameWaitCursor(visatApp.getMainFrame());
        ShowImageViewRGBAction.RGBBand[] rgbBands = null;
        boolean errorOccured = false;
        ProductSceneImage productSceneImage = null;
        try {
            pm.beginTask("Creating HSV image...", 2);
            final String[] rgbaExpressions = convertHSVToRGBExpressions(hsvExpressions);
            rgbBands = ShowImageViewRGBAction.allocateRgbBands(product, rgbaExpressions);

            productSceneImage = new ProductSceneImage(name, rgbBands[0].band,
                                                      rgbBands[1].band,
                                                      rgbBands[2].band,
                                                      visatApp.getPreferences(),
                                                      SubProgressMonitor.create(pm, 1));
            productSceneImage.initVectorDataCollectionLayer();
            productSceneImage.initMaskCollectionLayer();
        } catch (Exception e) {
            errorOccured = true;
            throw e;
        } finally {
            pm.done();
            if (rgbBands != null) {
                ShowImageViewRGBAction.releaseRgbBands(rgbBands, errorOccured);
            }
        }
        return productSceneImage;
    }

    private static void nomalizeHSVExpressions(final Product product, String[] hsvExpressions) {
        // normalize
        //range = max - min;
        //normvalue = min(max(((v- min)/range),0), 1);
        boolean modified = product.isModified();

        int i=0;
        for(String exp : hsvExpressions) {
            if(exp.isEmpty()) continue;

            final String checkForNoDataValue = "";//getCheckForNoDataExpression(product, exp);

            final Band virtBand = createVirtualBand(product, exp, "tmpVirtBand"+i);

            final Stx stx = virtBand.getStx(false, ProgressMonitor.NULL);
            if(stx != null) {
                final double min = stx.getMinimum();
                final double range = stx.getMaximum() - min;
                hsvExpressions[i] = checkForNoDataValue + "min(max(((("+exp+")- "+min+")/"+range+"), 0), 1)";
            }
            product.removeBand(virtBand);
            ++i;
        }
        product.setModified(modified);
    }

    private static String getCheckForNoDataExpression(final Product product, final String exp) {
        final String[] bandNames = product.getBandNames();
        StringBuilder checkForNoData = new StringBuilder("("+exp+" == NaN");
        if(StringUtils.contains(bandNames, exp)) {
            double nodatavalue = product.getBand(exp).getNoDataValue();
            checkForNoData.append(" or "+exp+" == "+nodatavalue);
        }
        checkForNoData.append(") ? NaN : ");

        return checkForNoData.toString();
    }

    public static Band createVirtualBand(final Product product, final String expression, final String name) {

        final VirtualBand virtBand = new VirtualBand(name,
                ProductData.TYPE_FLOAT64,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                expression);
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
        return virtBand;
    }

    private static String[] convertHSVToRGBExpressions(final String[] hsvExpressions) {

        final String h = hsvExpressions[0].isEmpty() ? "0" : hsvExpressions[0];
        final String s = hsvExpressions[1].isEmpty() ? "0" : hsvExpressions[1];
        final String v = hsvExpressions[2].isEmpty() ? "0" : hsvExpressions[2];

        // h,s,v in [0,1]
   /*   float rr = 0, gg = 0, bb = 0;
		float hh = (6 * h) % 6;
		int   c1 = (int) hh;                // floor((6*(h))%6)
		float c2 = hh - c1;                 // ((6*(h))%6)-floor((6*(h))%6)
		float x = (1 - s) * v;              // ((1-(s))*(v))
		float y = (1 - (s * c2)) * v;       // ((1-((s)*((6*(h))%6)-floor((6*(h))%6)))*(v))
		float z = (1 - (s * (1 - c2))) * v; // ((1-((s)*(1-((6*(h))%6)-floor((6*(h))%6))))*(v))
		switch (c1) {
			case 0: rr=v; gg=z; bb=x; break;
			case 1: rr=y; gg=v; bb=x; break;
			case 2: rr=x; gg=v; bb=z; break;
			case 3: rr=x; gg=y; bb=v; break;
			case 4: rr=z; gg=x; bb=v; break;
			case 5: rr=v; gg=x; bb=y; break;
		}
		int N = 256;
		int r = Math.min(Math.round(rr*N),N-1);
		int g = Math.min(Math.round(gg*N),N-1);
		int b = Math.min(Math.round(bb*N),N-1);
        */


        final String[] rgbExpressions = new String[3];
        rgbExpressions[0] = r.replace("(h)", '(' +h+ ')').replace("(s)", '(' +s+ ')').replace("(v)", '(' +v+ ')');
        rgbExpressions[1] = g.replace("(h)", '(' +h+ ')').replace("(s)", '(' +s+ ')').replace("(v)", '(' +v+ ')');
        rgbExpressions[2] = b.replace("(h)", '(' +h+ ')').replace("(s)", '(' +s+ ')').replace("(v)", '(' +v+ ')');
        return rgbExpressions;
    }

    private static String createSceneName(Product product, RGBImageProfile rgbImageProfile) {
        final StringBuilder nameBuilder = new StringBuilder(80);
        final String productRef = product.getProductRefString();
        if (productRef != null) {
            nameBuilder.append(productRef);
            nameBuilder.append(' ');
        }
        if (rgbImageProfile != null) {
            nameBuilder.append(rgbImageProfile.getName().replace("_", " "));
            nameBuilder.append(' ');
        }
        nameBuilder.append("HSV");

        return nameBuilder.toString();
    }
}