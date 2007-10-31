/*
 * $Id$
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public abstract class SingleTargetProductDialog extends ModalDialog {

    private TargetProductSelector targetProductSelector;
    private AppContext appContext;

    public SingleTargetProductDialog(AppContext appContext, String title, int buttonMask, String helpID) {
        super(appContext.getApplicationWindow(), title, buttonMask, helpID);
        this.appContext = appContext;
        targetProductSelector = new TargetProductSelector();
        String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        String saveDir = appContext.getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
        targetProductSelector.getModel().setProductDir(new File(saveDir));
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());
    }

    public SingleTargetProductDialog(AppContext appContext, String title, String helpID) {
        this(appContext, title, ModalDialog.ID_OK_CANCEL_HELP, helpID);
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public TargetProductSelector getTargetProductSelector() {
        return targetProductSelector;
    }

    @Override
    protected void onOK() {
        if (targetProductSelector.getModel().getProductName().isEmpty()) {
            showErrorDialog("Please specify a target product name.");
            targetProductSelector.getProductNameTextField().requestFocus();
            return;
        }
        String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        appContext.getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);

        Product targetProduct = null;
        try {
            targetProduct = createTargetProduct();
        } catch (Exception e) {
            showErrorDialog(e.getMessage());
        }
        if (targetProduct == null) {
            return;
        }

        targetProduct.setName(targetProductSelector.getModel().getProductName());
        targetProduct.setFileLocation(targetProductSelector.getModel().getProductFile());

        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            final ProgressMonitorSwingWorker worker = new ProductWriterSwingWorker(targetProduct);
            worker.executeWithBlocking();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            appContext.addProduct(targetProduct);
        }

        hide();
    }

    private class ProductWriterSwingWorker extends ProgressMonitorSwingWorker<Product, Object> {
        private final Product targetProduct;

        private ProductWriterSwingWorker(Product targetProduct) {
            super(getJDialog(), "Writing Target Product");
            this.targetProduct = targetProduct;
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            pm.beginTask("Writing...", model.isOpenInAppSelected() ? 100 : 95);
            Product product = null;
            try {
                WriteOp.writeProduct(targetProduct,
                                     model.getProductFile(),
                                     model.getFormatName(), SubProgressMonitor.create(pm, 95));
                if (model.isOpenInAppSelected()) {
                    product = ProductIO.readProduct(model.getProductFile(), null);
                    if (product == null) {
                        product = targetProduct;
                    }
                    pm.worked(5);
                }
            } finally {
                pm.done();
                if (product != targetProduct) {
                    targetProduct.dispose();
                }
            }
            return product;
        }

        @Override
        protected void done() {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            try {
                final Product targetProduct = get();
                if (model.isOpenInAppSelected()) {
                    appContext.addProduct(targetProduct);
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause());
            }
        }
    }

    protected abstract Product createTargetProduct() throws Exception;
}