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
package org.esa.beam.visat.actions.pin;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.SelectExportMethodDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Dialog;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This action exports pins and their surrounding pixels.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @deprecated since BEAM 4.9, replaced by module beam-pixel-extraction
 */
@Deprecated
public class ExportPinPixelsAction extends ExecCommand {

    private class PTL extends ProductTreeListenerAdapter {

        @Override
        public void productRemoved(final Product product) {
            if (selectedProduct == product) {
                selectedProduct.removeProductNodeListener(pnl);
                selectedProduct = null;
            }
        }
    }

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            checkEnabledState();
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            checkEnabledState();
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            checkEnabledState();
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            checkEnabledState();
        }
    }


    private static final String ERR_MSG_BASE = "Pin pixels cannot be exported:\n";
    private static final String COMMAND_NAME = "Export Pin Pixels";

    private ExportPinPixelsDialog dialog;
    private Product selectedProduct;
    private AtomicBoolean initialized = new AtomicBoolean();
    private ProductNodeListener pnl;

    @Override
    public void actionPerformed(final CommandEvent event) {
        exportPinPixels(getSelectedProduct());
    }

    @Override
    public void updateState(final CommandEvent event) {
        if (initialized.compareAndSet(false, true)) {
            pnl = new PNL();
            VisatApp.getApp().getProductTree().addProductTreeListener(new PTL());
        }

        final Product product = getSelectedProduct();
        if (selectedProduct != product) {
            if (selectedProduct != null) {
                selectedProduct.removeProductNodeListener(pnl);
            }
            selectedProduct = product;
            if (selectedProduct != null) {
                selectedProduct.addProductNodeListener(pnl);
            }
        }
        checkEnabledState();
    }

    private void checkEnabledState() {
        boolean enabled = false;
        if (selectedProduct != null) {
            enabled = selectedProduct.getPinGroup().getNodeCount() > 0;
        }
        setEnabled(enabled);
    }

    @Override
    public void updateComponentTreeUI() {
        if (dialog != null) {
            SwingUtilities.updateComponentTreeUI(dialog.getJDialog());
        }
    }


    /**
     * Performs the "Export Pin Pixels" command.
     *
     * @param product
     */
    private void exportPinPixels(Product product) {
        VisatApp visatApp = VisatApp.getApp();
        if (dialog == null) {
            dialog = new ExportPinPixelsDialog(visatApp);
        }

        // shows a dialog which lets the user specify the region he wants to export.
        final int dialogAnswer = dialog.show(product);

        // return if user pressed the Cancel-button
        if (ModalDialog.ID_OK != dialogAnswer) {
            return;
        }

        final Map<Placemark, Object[]> pinPixels = assignControllParameters(product);
        if (getNumPixelsToExport(pinPixels) == 0) {
            visatApp.showErrorDialog("There are no pixels to export."); /* I18N */
            return;
        }

        final Writer out = getOutputWriter(pinPixels, product);

        if (out == null) {
            return; // Export Pin Pixels was canceled by the export method dialog
        }

        final TabSeparatedPinPixelsWriter regionWriter = new TabSeparatedPinPixelsWriter(out, product.getBands(),
                                                                                         product.getGeoCoding());

        ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(),
                                                       "Export Pin Pixels",
                                                       Dialog.ModalityType.APPLICATION_MODAL);
        final SwingWorker swingWorker = createWorkerInstance(pinPixels, regionWriter, out, pm);

        // show wait-cursor
        UIUtils.setRootFrameWaitCursor(visatApp.getMainFrame());
        // show message in status bar
        visatApp.setStatusBarMessage("Exporting Pin pixels..."); /* I18N */

        // Start separate worker thread.
        swingWorker.execute();
    }

    /**
     * Gets a target Writer for the export data.
     */
    private Writer getOutputWriter(final Map<Placemark, Object[]> pinPixels, final Product product) {
        // Get export method from user
        final int method = getExportMethod(getNumPixelsToExport(pinPixels));
        VisatApp visatApp = VisatApp.getApp();

        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            return new StringWriter();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = promptForFile(visatApp, createDefaultFileName(product));
            if (file == null) {
                return null; // Cancel
            }
            try {
                return new FileWriter(file);
            } catch (IOException e) {
                visatApp.showErrorDialog(COMMAND_NAME, ERR_MSG_BASE + "Failed to create file '" + file + "':\n"
                                                       + e.getMessage()); /* I18N */
                return null; // Error
            }
        } else {
            return null; // Cancel
        }
    }

    /**
     * Opens a modal dialog that asks the user which method to use in order to export the pin
     * pixels.
     */
    private int getExportMethod(final int numPixels) {
        int result;

        final String questionText = "How do you want to export the pixel values?\n"; /* I18N */

        final String numPixelsText;
        if (numPixels == 1) {
            numPixelsText = "One pin pixel will be exported.\n"; /* I18N */
        } else {
            numPixelsText = numPixels + " pin pixels will be exported.\n"; /* I18N */
        }
        result = SelectExportMethodDialog.run(VisatApp.getApp().getMainFrame(),
                                              getWindowTitle(),
                                              questionText + numPixelsText,
                                              getHelpId());

        return result;
    }

    /**
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp the VISAT application
     *
     * @return the selected file, <code>null</code> means "Cancel"
     */
    private File promptForFile(final VisatApp visatApp, final String defaultFileName) {
        return visatApp.showFileSaveDialog(COMMAND_NAME,
                                           false,
                                           null,
                                           ".txt",
                                           defaultFileName,
                                           getCommandID() + ".lastDir");
    }

    /**
     * Creates the export Data. Can be <code>null</code> if an error occurs while reading the
     * product data or user input.
     *
     * @param product
     */
    private Map<Placemark, Object[]> assignControllParameters(Product product) {

        final Placemark[] exportPins;
        if (dialog.isExportSelectedPinsOnly()) {
            exportPins = VisatApp.getApp().getSelectedProductSceneView().getSelectedPins();
        } else {
            ProductNodeGroup<Placemark> pinGroup = product.getPinGroup();
            exportPins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        }

        final int regionSize = dialog.getRegionSize();
        String expression = null;
        boolean useExpressionAsFilter = true;

        if (dialog.isUseExpression()) {
            expression = dialog.getExpression();
            useExpressionAsFilter = dialog.isUseExpressionAsFilter();
        }

        try {
            return generateOutputData(regionSize, expression, exportPins, useExpressionAsFilter, product);
        } catch (IOException e) {
            VisatApp.getApp().showErrorDialog("An I/O error occurred:\n" + e.getMessage()); /* I18N */
            return null;
        } catch (ParseException e) {
            VisatApp.getApp().showErrorDialog(
                    "Please check the expression you have entered.\nIt is not valid."); /* I18N */
            return null;
        }
    }

    private SwingWorker createWorkerInstance(final Map<Placemark, Object[]> regions,
                                             final TabSeparatedPinPixelsWriter regionWriter,
                                             final Writer out, final ProgressMonitor pm) {
        // Create a progress monitor and adds them to the progress controller pool in order to show
        // export progress

        // Create a new swing worker instance (as instance of an anonymous class).
        // When the swing worker's start() method is called, a new separate thread is started.
        // The swing worker's construct() method is executed in that thread, so that VISAT can keep
        // on handling user events.
        return new SwingWorker() {

            /**
             * Compute the value to be returned by the <code>get</code> method. This method is
             * executed in a separate worker thread.
             */
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    final boolean success = regionWriter.writePlacemarkPixels(dialog.getRegionSize(), dialog
                            .getExpression(), regions, pm);
                    if (success) {
                        maybeCopyToClipboard(out);
                    }
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread) after the
             * <code>construct</code> method has returned.
             */
            @Override
            protected void done() {
                VisatApp visatApp = VisatApp.getApp();

                // clear status bar
                visatApp.clearStatusBarMessage();
                // show default-cursor
                UIUtils.setRootFrameDefaultCursor(visatApp.getMainFrame());
                // On error, show error message
                try {
                    if (get() instanceof Exception) {
                        final Exception e = (Exception) get();
                        visatApp.showErrorDialog(COMMAND_NAME, ERR_MSG_BASE + e.getMessage());
                    }
                } catch (InterruptedException e) {
                    // ignore, should not come here
                } catch (ExecutionException e) {
                    // ignore, should not come here
                }
            }

        };
    }

    private static void maybeCopyToClipboard(final Writer out) {
        if (out instanceof StringWriter) {
            final StringWriter stringWriter = (StringWriter) out;
            SystemUtils.copyToClipboard(stringWriter.toString());
        }
    }

    /**
     * Returns the product the user has selected in beam. Can be <code>null</code>.
     */
    private static Product getSelectedProduct() {
        return VisatApp.getApp().getSelectedProduct();
    }

    /**
     * Generates the output data as a hashmap. For each pin in the <code>pins</code> array the relevant pixel
     * coordinates around it are stored as a point array in a hashmap. The respective pin is taken
     * as key to access the pixel coordinates.
     *
     * @param size                  the size of the pixel region to be exported
     * @param expression            an arithmetic expression for classifying pixel relevance
     * @param pins                  the pins defining the center of the pixels regions
     * @param useExpressionAsFilter toggle between filter pixels or mark relevance in an additional column
     * @param product               the product that contains these pins
     *
     * @return a map providing the pixel coordinates
     *
     * @throws java.io.IOException
     * @throws com.bc.jexp.ParseException
     */
    private Map<Placemark, Object[]> generateOutputData(final int size, final String expression, final Placemark[] pins,
                                                        final boolean useExpressionAsFilter,
                                                        final Product product) throws IOException, ParseException {
        Map<Placemark, Object[]> outputData = new HashMap<Placemark, Object[]>();
        for (int i = 0; i < pins.length; i++) {
            Point actualPin = new Point((int) pins[i].getPixelPos().x, (int) pins[i].getPixelPos().y);
            PinPixelsGenerator pinPixels = new PinPixelsGenerator(product);
            if (useExpressionAsFilter) {
                Point[] pixels = pinPixels.generateQuadricPixelRegion(actualPin, size, expression);
                outputData.put(pins[i], pixels);
            } else {
                Point[] pixels = pinPixels.generateQuadricPixelRegion(actualPin, size);
                Boolean[] pixelRelevanceInformation = pinPixels.getRelevanceInformation(pixels, expression);
                Object[] pixelData = {pixels, pixelRelevanceInformation};
                outputData.put(pins[i], pixelData);
            }
        }
        return outputData;
    }

    /**
     * Calculates the number of pixels stored in the hashMap.
     *
     * @param pinPixels the hashmap providing the pixel coordinates
     *
     * @return the number of pixels stored in the hashMap
     */
    private static int getNumPixelsToExport(final Map<Placemark, Object[]> pinPixels) {
        int numPixels = 0;
        for (Map.Entry<Placemark, Object[]> entry : pinPixels.entrySet()) {
            Object[] pixelData = entry.getValue();
            if (pixelData != null && pixelData instanceof Point[]) {
                Point[] pixels = (Point[]) pixelData;
                numPixels += pixels.length;
            } else if (pixelData != null) {
                Point[] pixels = (Point[]) pixelData[0];
                if (pixels != null) {
                    numPixels += pixels.length;
                }
            }
        }
        return numPixels;

    }

    /**
     * Creates the default filename for the exported data.
     */
    private static String createDefaultFileName(final Product product) {
        return FileUtils.getFilenameWithoutExtension(product.getName()) + "_PinPixels.txt";
    }

    private static String getWindowTitle() {
        return VisatApp.getApp().getAppName() + " - " + COMMAND_NAME;
    }

}
