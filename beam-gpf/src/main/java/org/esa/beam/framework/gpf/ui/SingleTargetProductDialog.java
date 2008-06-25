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
import com.sun.media.jai.codec.PNGEncodeParam;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JOptionPane;
import javax.swing.AbstractButton;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public abstract class SingleTargetProductDialog extends ModelessDialog {

    private TargetProductSelector targetProductSelector;
    private AppContext appContext;

    public SingleTargetProductDialog(AppContext appContext, String title, String helpID) {
        this(appContext, title, ID_APPLY_CLOSE_HELP, helpID);
    }

    public SingleTargetProductDialog(AppContext appContext, String title, int buttonMask, String helpID) {
        super(appContext.getApplicationWindow(), title, buttonMask, helpID);
        this.appContext = appContext;
        targetProductSelector = new TargetProductSelector();
        String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        String saveDir = appContext.getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
        targetProductSelector.getModel().setProductDir(new File(saveDir));
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());
        targetProductSelector.getModel().getValueContainer().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                 if (evt.getPropertyName().equals("saveToFileSelected") ||
                         evt.getPropertyName().equals("openInAppSelected")) {
                     updateApplyButton();
                 }
            }
        });
        updateApplyButton();
    }

    private void updateApplyButton() {
        boolean save = targetProductSelector.getModel().isSaveToFileSelected();
        boolean open = targetProductSelector.getModel().isOpenInAppSelected();

        AbstractButton button = getButton(ID_APPLY);
        if (save && open) {
            button.setText("Save & Open");
            button.setMnemonic('S');
            button.setEnabled(true);
        } else if (save) {
            button.setText("Save");
            button.setMnemonic('S');
            button.setEnabled(true);
        } else if (open) {
            button.setText("Open");
            button.setMnemonic('O');
            button.setEnabled(true);
        } else {
            button.setText("Save");
            button.setMnemonic('S');
            button.setEnabled(false);
        }

    }

    public AppContext getAppContext() {
        return appContext;
    }

    public TargetProductSelector getTargetProductSelector() {
        return targetProductSelector;
    }

    @Override
    protected void onApply() {
        final String productName = targetProductSelector.getModel().getProductName();
        if (productName == null || productName.isEmpty()) {
            showErrorDialog("Please specify a target product name.");
            targetProductSelector.getProductNameTextField().requestFocus();
            return;
        }
        File productFile = targetProductSelector.getModel().getProductFile();
        if (productFile.exists() && targetProductSelector.getModel().isSaveToFileSelected()) {
            String message = "The specified output file\n\"{0}\"\n already exists.\n\n" + 
                    "Do you want to overwrite the existing file?";
            String formatedMessage = MessageFormat.format(message, productFile.getAbsolutePath());
            final int answer = JOptionPane.showConfirmDialog(getJDialog(), formatedMessage,
                    getJDialog().getTitle(), JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
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

        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            targetProduct.setFileLocation(productFile);
            final ProgressMonitorSwingWorker worker = new ProductWriterSwingWorker(targetProduct);
            worker.executeWithBlocking();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            // todo - check for existance
            appContext.getProductManager().addProduct(targetProduct);
        }
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
                    appContext.getProductManager().addProduct(targetProduct);
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