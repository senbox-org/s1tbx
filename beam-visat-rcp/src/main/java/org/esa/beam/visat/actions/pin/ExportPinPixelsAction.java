/*
 * $Id: ExportPinPixelsAction.java,v 1.1 2007/04/19 10:16:11 marcop Exp $
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
package org.esa.beam.visat.actions.pin;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.SelectExportMethodDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JOptionPane;
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
import java.text.MessageFormat;

/**
 * This action exports pins and thier surrounding pixels.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ExportPinPixelsAction extends ExecCommand {

    private static final String ERR_MSG_BASE = "Pin pixels cannot be exported:\n";
    private static final String COMMAND_NAME = "Export Pin Pixels";

    private ExportPinPixelsDialog dialog;
    private Product product;


    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final CommandEvent event) {
        exportPinPixels();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateState(final CommandEvent event) {
        boolean enabled = false;
        final Product product = getSelectedProduct();
        if (product != null) {
            enabled = product.getPinGroup().getNodeCount() > 0;
        }
        setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateComponentTreeUI() {
        if (dialog != null) {
            SwingUtilities.updateComponentTreeUI(dialog.getJDialog());
        }
    }


    /**
     * Performs the "Export Pin Pixels" command.
     */
    private void exportPinPixels() {
        product = getSelectedProduct();

        ensureThatAPinIsSelected(product);
        final Pin selectedPin = product.getPinGroup().getSelectedNode();
        VisatApp visatApp = VisatApp.getApp();
        if (dialog == null) {
            dialog = new ExportPinPixelsDialog(visatApp, product);
        }

        // shows a dialog which lets the user specify the region he wants to export.
        final int dialogAnswer = dialog.show(selectedPin.getLabel(), product);

        // return if user pressed the Cancel-button
        if (ModalDialog.ID_OK != dialogAnswer) {
            return;
        }

        final HashMap pinPixels = assignControllParameters();
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
     * Ensures that a pin is selected in the given product.
     */
    private static void ensureThatAPinIsSelected(final Product product) {
        if (product.getPinGroup().getSelectedNode() == null) {
            product.getPinGroup().get(0).setSelected(true);
        }
    }

    /**
     * Gets a target Writer for the export data.
     */
    private Writer getOutputWriter(final HashMap pinPixels, final Product product) {
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
            numPixelsText = "One pixel will be exported.\n"; /* I18N */
        } else {
            numPixelsText = numPixels + " pixels will be exported.\n"; /* I18N */
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
        // Loop while the user does not want to overwrite a selected, existing file or if the user
        // presses "Cancel"

        File file = null;
        while (file == null) {
            file = visatApp.showFileSaveDialog(COMMAND_NAME, false, null, ".txt", defaultFileName, getCommandID()
                                                                                                   + ".lastDir");
            if (file == null) {
                return null; // Cancel
            } else if (file.exists()) {
                final String message = MessageFormat.format("The file ''{0}'' already exists.\nOverwrite it?", file);
                final String title = MessageFormat.format("{0} - {1}", visatApp.getAppName(), COMMAND_NAME);
                final int status = JOptionPane.showConfirmDialog(visatApp.getMainFrame(),
                                                                 message,
                                                                 title,
                                                                 JOptionPane.YES_NO_CANCEL_OPTION,
                                                                 JOptionPane.WARNING_MESSAGE);
                if (status == JOptionPane.CANCEL_OPTION) {
                    return null; // Cancel
                } else if (status == JOptionPane.NO_OPTION) {
                    file = null; // No, do not overwrite, let user select
                    // other file
                }
            }
        }
        return file;
    }

    /**
     * Creates the export Data. Can be <code>null</code> if an error occures while reading the
     * product data or user input.
     */
    private HashMap assignControllParameters() {

        final Pin[] exportPins;
        if (dialog.isExportSelectedPinOnly()) {
            exportPins = new Pin[]{product.getPinGroup().getSelectedNode()};
        } else {
            ProductNodeGroup<Pin> pinGroup = product.getPinGroup();
            exportPins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
        }

        final int regionSize = dialog.getRegionSize();
        String expression = null;
        boolean useExpressionAsFilter = true;

        if (dialog.isUseExpression()) {
            expression = dialog.getExpression();
            useExpressionAsFilter = dialog.isUseExpressionAsFilter();
        }

        HashMap exportData = null;

        try {
            exportData = generateOutputData(regionSize, expression, exportPins, useExpressionAsFilter);
        } catch (IOException e) {
            VisatApp.getApp().showErrorDialog("An I/O error occured:\n" + e.getMessage()); /* I18N */
            return null;
        } catch (ParseException e) {
            VisatApp.getApp().showErrorDialog(
                    "Please check the expression you have entered.\nIt is not valid."); /* I18N */
            return null;
        }

        return exportData;
    }

    private SwingWorker createWorkerInstance(final HashMap regions, final TabSeparatedPinPixelsWriter regionWriter,
                                             final Writer out, final ProgressMonitor pm) {
        // Create a progress monitor and adds them to the progress controller pool in order to show
        // export progress

        // Create a new swing worker instance (as instance of an anonymous class).
        // When the swing worker's start() method is called, a new separate thread is started.
        // The swing worker's construct() method is executed in that thread, so that VISAT can keep
        // on handling user events.
        return (SwingWorker) new SwingWorker() {

            /**
             * Compute the value to be returned by the <code>get</code> method. This method is
             * executed in a separate worker thread.
             */
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    final boolean success = regionWriter.writePinPixels(dialog.getRegionSize(), dialog
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
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            return view.getProduct();
        }
        return null;
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
     *
     * @return a hashmap providing the pixel coordinates
     *
     * @throws java.io.IOException
     * @throws com.bc.jexp.ParseException
     */
    private HashMap generateOutputData(final int size, final String expression, final Pin[] pins,
                                       final boolean useExpressionAsFilter) throws IOException, ParseException {
        HashMap<Pin, Object[]> outputData = new HashMap<Pin, Object[]>();
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
    private static int getNumPixelsToExport(final HashMap pinPixels) {
        int numPixels = 0;
        for (Object o : pinPixels.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object pixelDataObject = entry.getValue();
            if (pixelDataObject != null && pixelDataObject instanceof Point[]) {
                Point[] pixels = (Point[]) entry.getValue();
                if (pixels != null) {
                    numPixels += pixels.length;
                }
            } else if (pixelDataObject != null) {
                Object[] pixelData = (Object[]) entry.getValue();
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
