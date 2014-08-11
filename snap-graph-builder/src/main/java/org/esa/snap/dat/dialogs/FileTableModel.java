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

import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.io.File;

/**
 * Interfrace for FileModel
 */
public interface FileTableModel extends TableModel {

    public void addFile(final File file);

    public void addFile(final ProductEntry entry);

    public void removeFile(final int index);

    public File[] getFileList();

    public void clear();

    public void setColumnWidths(final TableColumnModel columnModel);

    public File getFileAt(final int index);

    public File[] getFilesAt(final int[] indices);

    public int getIndexOf(final File file);

    public void move(final int oldIndex, final int newIndex);
}
