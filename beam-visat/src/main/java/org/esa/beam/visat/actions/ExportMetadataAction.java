package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.ui.SelectExportMethodDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductMetadataTable;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;
import java.awt.Dialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExportMetadataAction extends ExecCommand {

    private static final String ERR_MSG_BASE = "Metadata could not be exported:\n";
    private static final String DLG_TITLE = "Export Product Metadata";

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        exportMetadata(event);
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(CommandEvent event) {
        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        setEnabled(view instanceof ProductMetadataView);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "Export Metadata" command
    /////////////////////////////////////////////////////////////////////////

    /**
     * Performs the actual "Export Metadata" command.
     */
    private static void exportMetadata(CommandEvent event) {

        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        if (!(view instanceof ProductMetadataView)) {
            return;
        }

        final ProductMetadataView productMetadataView = (ProductMetadataView) view;
        final ProductMetadataTable metadataTable = productMetadataView.getMetadataTable();
        final TableModel metadataTableModel = metadataTable.getModel();

        final String questionText = "How do you want to export the metadata?\n"; /*I18N*/
        final String numRowsText;
        if (metadataTableModel.getRowCount() == 1) {
            numRowsText = "One data row will be exported.\n"; /*I18N*/
        } else {
            numRowsText = metadataTableModel.getRowCount() + " data rows will be exported.\n"; /*I18N*/
        }
        // Get export method from user
        final int method = SelectExportMethodDialog.run(VisatApp.getApp().getMainFrame(), getWindowTitle(),
                                                        questionText + numRowsText, event.getCommand().getHelpId());

        final PrintWriter out;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            out = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = promptForFile(VisatApp.getApp(), createDefaultFileName(productMetadataView));
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                VisatApp.getApp().showErrorDialog(DLG_TITLE,
                                                  ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage()); /*I18N*/
                return; // Error
            }
            out = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }

        // Create a progress monitor and adds them to the progress controller pool in order to show export progress
        // Create a new swing worker instance (as instance of an anonymous class).
        // When the swing worker's start() method is called, a new separate thread is started.
        // The swing worker's construct() method is executed in that thread, so that
        // VISAT can keep on handling user events.
        //
        final SwingWorker swingWorker = new SwingWorker<Exception, Object>() {

            @Override
            protected Exception doInBackground() throws Exception {
                boolean success;
                Exception returnValue = null;
                try {
                    ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(), DLG_TITLE,
                                                                   Dialog.ModalityType.APPLICATION_MODAL);
                    success = exportMetadata(out, metadataTable, pm);
                    if (success && clipboardText != null) {
                        SystemUtils.copyToClipboard(clipboardText.toString());
                        clipboardText.setLength(0);
                    }
                } catch (Exception e) {
                    returnValue = e;
                } finally {
                    out.close();
                }
                return returnValue;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread) after the <code>construct</code> method
             * has returned.
             */
            @Override
            public void done() {
                // clear status bar
                VisatApp.getApp().clearStatusBarMessage();
                // show default-cursor
                UIUtils.setRootFrameDefaultCursor(VisatApp.getApp().getMainFrame());
                // On error, show error message
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception != null) {
                    VisatApp.getApp().showErrorDialog(DLG_TITLE,
                                                      ERR_MSG_BASE + exception.getMessage());
                }
            }
        };

        // show wait-cursor
        UIUtils.setRootFrameWaitCursor(VisatApp.getApp().getMainFrame());
        // show message in status bar
        VisatApp.getApp().setStatusBarMessage("Exporting Product Metadata..."); /*I18N*/

        // Start separate worker thread.
        swingWorker.execute();
    }

    private static String createDefaultFileName(ProductMetadataView productMetadataView) {
        return FileUtils.getFilenameWithoutExtension(productMetadataView.getProduct().getName()) +
               "_" +
               productMetadataView.getMetadataElement().getName() +
               ".txt";
    }

    private static String getWindowTitle() {
        return VisatApp.getApp().getAppName() + " - " + DLG_TITLE;
    }

    /**
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp the VISAT application
     *
     * @return the selected file, <code>null</code> means "Cancel"
     */
    private static File promptForFile(final VisatApp visatApp, String defaultFileName) {
        // Loop while the user does not want to overwrite a selected, existing file
        // or if the user presses "Cancel"
        //
        File file = null;
        while (file == null) {
            file = visatApp.showFileSaveDialog(DLG_TITLE,
                                               false,
                                               null,
                                               ".txt",
                                               defaultFileName,
                                               "exportMetadata.lastDir");
            if (file == null) {
                return null; // Cancel
            } else if (file.exists()) {
                int status = JOptionPane.showConfirmDialog(visatApp.getMainFrame(),
                                                           "The file '" + file + "' already exists.\n" + /*I18N*/
                                                           "Overwrite it?", /*I18N*/
                                                                            "VISAT - " + DLG_TITLE,
                                                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                                                            JOptionPane.WARNING_MESSAGE);
                if (status == JOptionPane.CANCEL_OPTION) {
                    return null; // Cancel
                } else if (status == JOptionPane.NO_OPTION) {
                    file = null; // No, do not overwrite, let user select other file
                }
            }
        }
        return file;
    }

    /**
     * Writes all pixel values of the given product within the given ROI to the specified out.
     *
     * @param out the data output writer
     *
     * @return <code>true</code> for success, <code>false</code> if export has been terminated (by user)
     */
    private static boolean exportMetadata(final PrintWriter out,
                                          final ProductMetadataTable metadataTable,
                                          ProgressMonitor pm) {
        final TableModel metadataTableModel = metadataTable.getModel();
        writeHeaderLine(out, metadataTable);
        final int rowCount = metadataTableModel.getRowCount();
        pm.beginTask("Writing data rows...", rowCount);
        try {
            for (int i = 0; i < rowCount; i++) {
                writeDataLine(out, metadataTable, i);
                pm.worked(1);
                if (pm.isCanceled()) {
                    return false;
                }
            }
        } finally {
            pm.done();
        }

        return true;
    }

    /**
     * Writes the header line of the dataset to be exported.
     */
    private static void writeHeaderLine(final PrintWriter out,
                                        final ProductMetadataTable metadataTable) {
        final TableModel metadataTableModel = metadataTable.getModel();
        final int columnCount = metadataTableModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            out.print(metadataTableModel.getColumnName(i));
            if (i < columnCount - 1) {
                out.print("\t");
            }
        }
        out.print("\n");
    }

    /**
     * Writes a data line of the dataset to be exported for the given pixel position.
     */
    private static void writeDataLine(final PrintWriter out,
                                      final ProductMetadataTable metadataTable,
                                      int row) {
        final TableModel metadataTableModel = metadataTable.getModel();
        final int columnCount = metadataTableModel.getColumnCount();
        ProductMetadataTable.ElementRef elementRef;
        for (int i = 0; i < columnCount; i++) {
            elementRef = (ProductMetadataTable.ElementRef) metadataTableModel.getValueAt(row, i);
            out.print(metadataTable.getElementText(elementRef, i));
            if (i < columnCount - 1) {
                out.print("\t");
            }
        }
        out.print("\n");
    }

}
