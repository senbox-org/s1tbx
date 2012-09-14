package org.esa.nest.dat.dialogs;

import org.esa.nest.db.ProductEntry;

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
