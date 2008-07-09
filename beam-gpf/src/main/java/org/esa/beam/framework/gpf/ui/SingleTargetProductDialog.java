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
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.SuppressibleOptionPane;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

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
                    updateRunButton();
                }
            }
        });
        AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        updateRunButton();
    }

    private void updateRunButton() {
        AbstractButton button = getButton(ID_APPLY);
        boolean save = targetProductSelector.getModel().isSaveToFileSelected();
        boolean open = targetProductSelector.getModel().isOpenInAppSelected();
        String toolTipText = "";
        boolean enabled = true;
        if (save && open) {
            toolTipText = "Save target product and open it in " + getAppContext().getApplicationName();
        } else if (save) {
            toolTipText = "Save target product";
        } else if (open) {
            toolTipText = "Open target product in " + getAppContext().getApplicationName();
        } else {
            enabled = false;
        }
        button.setToolTipText(toolTipText);
        button.setEnabled(enabled);
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public TargetProductSelector getTargetProductSelector() {
        return targetProductSelector;
    }

    @Override
    protected void onApply() {
        if (!canApply()) {
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
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            targetProduct.setFileLocation(targetProductSelector.getModel().getProductFile());
            final ProgressMonitorSwingWorker worker = new ProductWriterSwingWorker(targetProduct);
            worker.executeWithBlocking();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            appContext.getProductManager().addProduct(targetProduct);
            showOpenInAppInfo();
        }
    }

    private boolean canApply() {
        final String productName = targetProductSelector.getModel().getProductName();
        if (productName == null || productName.isEmpty()) {
            showErrorDialog("Please specify a target product name.");
            targetProductSelector.getProductNameTextField().requestFocus();
            return false;
        }

        if (targetProductSelector.getModel().isOpenInAppSelected()) {
            final Product existingProduct = appContext.getProductManager().getProduct(productName);
            if (existingProduct != null) {
                String message = MessageFormat.format(
                        "A product with the name ''{0}'' is already opened in {1}.\n\n" +
                                "Do you want to continue?",
                        productName, appContext.getApplicationName());
                final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                 getTitle(), JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            File productFile = targetProductSelector.getModel().getProductFile();
            if (productFile.exists()) {
                String message = MessageFormat.format(
                        "The specified output file\n\"{0}\"\n already exists.\n\n" +
                                "Do you want to overwrite the existing file?",
                        productFile.getPath());
                final int answer = JOptionPane.showConfirmDialog(getJDialog(), message,
                                                                 getTitle(), JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showSaveInfo() {
        final String message = MessageFormat.format(
                "The target product has been successfully written to\n{0}",
                getTargetProductSelector().getModel().getProductFile());
        showSuppressibleInformationDialog(message, "saveInfo");
    }

    private void showOpenInAppInfo() {
        final String message = MessageFormat.format(
                "The target product has successfully been created and opened in {0}.\n\n" +
                        "Actual processing of source to target data will be performed only on demand,\n" +
                        "for example, if the target product is saved or an image view is opened.",
                appContext.getApplicationName());
        showSuppressibleInformationDialog(message, "openInAppInfo");
    }

    private void showSaveAndOpenInAppInfo() {
        final String message = MessageFormat.format(
                "The target product has been successfully written to\n{0}\n" +
                        "and has been opened in {1}.",
                getTargetProductSelector().getModel().getProductFile(),
                appContext.getApplicationName());
        showSuppressibleInformationDialog(message, "saveAndOpenInAppInfo");
    }


    /**
     * Shows an information dialog on top of this dialog.
     * The shown dialog will contain a user option allowing to not show the message anymore.
     *
     * @param infoMessage  The message.
     * @param propertyName The (simple) property name used to store the user option in the application's preferences.
     */
    public void showSuppressibleInformationDialog(String infoMessage, String propertyName) {
        final SuppressibleOptionPane optionPane = new SuppressibleOptionPane(appContext.getPreferences());
        optionPane.showMessageDialog(getClass().getName() + "." + propertyName,
                                     getJDialog(),
                                     infoMessage,
                                     getTitle(),
                                     JOptionPane.INFORMATION_MESSAGE);
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
                    showSaveAndOpenInAppInfo();
                } else {
                    showSaveInfo();
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