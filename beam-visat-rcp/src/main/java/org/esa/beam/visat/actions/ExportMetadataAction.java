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
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductNode;
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
import java.text.MessageFormat;

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

    private static void exportMetadata(CommandEvent event) {

        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        if (!(view instanceof ProductMetadataView)) {
            return;
        }

        final ProductMetadataView productMetadataView = (ProductMetadataView) view;
        final ProductMetadataTable metadataTable = productMetadataView.getMetadataTable();

        final String msgText = "How do you want to export the metadata?\n" +
                               "Element '" + metadataTable.getMetadataElement().getName() + "' will be exported.\n"; /*I18N*/
        // Get export method from user
        final int method = SelectExportMethodDialog.run(VisatApp.getApp().getMainFrame(), getWindowTitle(),
                                                        msgText, event.getCommand().getHelpId());

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
                    final MetadataExporter exporter = new MetadataExporter(metadataTable);
                    success = exporter.exportMetadata(out, pm);
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
     * @param visatApp        An instance of the VISAT application.
     * @param defaultFileName The default file name.
     *
     * @return The selected file, <code>null</code> means "Cancel".
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
                                                           "Overwrite it?",
                                                           MessageFormat.format("{0} - {1}", visatApp.getAppName(), DLG_TITLE),
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

    private static class MetadataExporter {

        private final ProductMetadataTable metadataTable;
        private final MetadataElement rootElement;

        private MetadataExporter(ProductMetadataTable metadataTable) {
            this.metadataTable = metadataTable;
            rootElement = metadataTable.getMetadataElement();
        }

        public boolean exportMetadata(final PrintWriter out, ProgressMonitor pm) {
            pm.beginTask("Export Metadata", 1);
            try {
                writeHeaderLine(out, metadataTable);
                writeAttributes(out, rootElement);
                pm.worked(1);
            } finally {
                pm.done();
            }
            return true;
        }


        private void writeHeaderLine(final PrintWriter out,
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


        private void writeAttributes(PrintWriter out, MetadataElement element) {
            final MetadataAttribute[] attributes = element.getAttributes();
            for (MetadataAttribute attribute : attributes) {
                out.print(createAttributeName(attribute) + "\t");
                out.print(attribute.getData().getElemString() + "\t");
                out.print(attribute.getUnit() + "\t");
                out.print(attribute.getDescription() + "\t\n");
            }
            final MetadataElement[] subElements = element.getElements();
            for (MetadataElement subElement : subElements) {
                writeAttributes(out, subElement);
            }
        }

        private String createAttributeName(MetadataAttribute attribute) {
            StringBuilder sb = new StringBuilder();
            MetadataElement metadataElement = attribute.getParentElement();
            if (metadataElement != null) {
                prependParentName(metadataElement, sb);
            }
            sb.append(attribute.getName());
            return sb.toString();
        }

        private void prependParentName(MetadataElement element, StringBuilder sb) {
            final MetadataElement owner = element.getParentElement();
            if (owner != null) {
                if (owner != rootElement) {
                    prependParentName(owner, sb);
                } else if (owner.getName() != null) {
                    sb.insert(0, owner.getName()).append(".");
                }
            }
            if (element.getName() != null) {
                sb.append(element.getName()).append(".");
            }
        }
    }
}
