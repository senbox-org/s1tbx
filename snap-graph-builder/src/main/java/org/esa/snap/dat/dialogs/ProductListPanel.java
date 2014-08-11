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

import org.esa.snap.db.ProductEntry;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * List of Products
 */
public class ProductListPanel extends JPanel {

    private final FileTable table = new FileTable();

    public ProductListPanel(final String title, final FileTableModel fileModel) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        table.setModel(fileModel);
        final JScrollPane scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public File[] getSelectedFiles() {
        return table.getModel().getFilesAt(table.getSelectedRows());
    }

    public File[] getFileList() {
        return table.getFileList();
    }

    public Object getValueAt(final int r, final int c) {
        return table.getModel().getValueAt(r, c);
    }

    public void setProductFileList(final File[] productFileList) {
        table.setFiles(productFileList);
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        table.setProductEntries(productEntryList);
    }
}