/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.dialogs;

import org.esa.beam.visat.VisatApp;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.util.ClipboardUtils;
import org.esa.snap.util.ProductFunctions;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Table for listing product files
 */
public class FileTable extends JTable {

    private final FileTableModel fileModel;

    public FileTable() {
        this(new FileModel());
    }

    public FileTable(FileTableModel fileModel) {
        if (fileModel == null) {
            fileModel = new FileModel();
        }
        this.fileModel = fileModel;
        this.setModel(fileModel);

        final int width = 500;
        final int height = 100;
        setPreferredScrollableViewportSize(new Dimension(width, height));
        fileModel.setColumnWidths(getColumnModel());
        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setDragEnabled(true);
        setComponentPopupMenu(createTablePopup());
        setTransferHandler(new ProductSetTransferHandler(fileModel));
    }

    public void setFiles(final File[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (File file : fileList) {
                fileModel.addFile(file);
            }
        }
    }

    public void setFiles(final String[] fileList) {
        if (fileList != null) {
            fileModel.clear();
            for (String str : fileList) {
                fileModel.addFile(new File(str));
            }
        }
    }

    public void setProductEntries(final ProductEntry[] productEntryList) {
        if (productEntryList != null) {
            fileModel.clear();
            for (ProductEntry entry : productEntryList) {
                fileModel.addFile(entry);
            }
        }
    }

    public int getFileCount() {
        int cnt = fileModel.getRowCount();
        if (cnt == 1) {
            File file = fileModel.getFileAt(0);
            if (file.getName().isEmpty())
                return 0;
        }
        return cnt;
    }

    public File[] getFileList() {
        return fileModel.getFileList();
    }

    public FileTableModel getModel() {
        return fileModel;
    }

    private JPopupMenu createTablePopup() {
        final JPopupMenu popup = new JPopupMenu();
        final JMenuItem pastelItem = new JMenuItem("Paste");
        pastelItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                paste();
            }
        });
        popup.add(pastelItem);

        return popup;
    }

    private void paste() {
        try {
            final File[] fileList = ClipboardUtils.getClipboardFileList();
            if (fileList != null) {
                setFiles(fileList);
            }
        } catch (Exception e) {
            if (VisatApp.getApp() != null) {
                VisatApp.getApp().showErrorDialog("Unable to paste from clipboard: " + e.getMessage());
            }
        }
    }

    public static class ProductSetTransferHandler extends TransferHandler {

        private final FileTableModel fileModel;

        public ProductSetTransferHandler(FileTableModel model) {
            fileModel = model;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        /**
         * Perform the actual import
         */
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            // Get the string that is being dropped.
            final Transferable t = info.getTransferable();
            String data;
            try {
                data = (String) t.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                return false;
            }

            // Wherever there is a newline in the incoming data,
            // break it into a separate item in the list.
            final String[] values = data.split("\n");

            // Perform the actual import.
            for (String value : values) {

                final File file = new File(value);
                if (file.exists()) {
                    if (ProductFunctions.isValidProduct(file)) {
                        fileModel.addFile(file);
                    }
                }
            }
            return true;
        }

        // export
        @Override
        protected Transferable createTransferable(JComponent c) {
            final JTable table = (JTable) c;
            final int[] rows = table.getSelectedRows();

            final StringBuilder listStr = new StringBuilder(256);
            for (int row : rows) {
                final File file = fileModel.getFileAt(row);
                listStr.append(file.getAbsolutePath());
                listStr.append('\n');
            }
            if (rows.length != 0) {
                return new StringSelection(listStr.toString());
            }
            return null;
        }
    }
}
