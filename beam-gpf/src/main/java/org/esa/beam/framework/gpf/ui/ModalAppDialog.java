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
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;

import java.awt.Window;
import java.util.concurrent.ExecutionException;


public abstract class ModalAppDialog extends ModalDialog {

    private TargetProductSelector targetProductSelector;
    private AppContext appContext;

    public ModalAppDialog(AppContext appContext, String title, int buttonMask, String helpID) {
        super(appContext.getApplicationWindow(), title, buttonMask, helpID);
        this.appContext = appContext;
        targetProductSelector = new TargetProductSelector();
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());
    }

    public ModalAppDialog(AppContext appContext, Window parent, String title, String helpID) {
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
            WriteOp.writeProduct(targetProduct,
                                 model.getProductFile(),
                                 model.getFormatName(), pm);
            targetProduct.dispose();
            if (model.isOpenInAppSelected()) {
                return ProductIO.readProduct(model.getProductFile(), null);
            }
            return null;
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