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

package org.esa.beam.visat.actions.pgrab.model.dataprovider;

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.util.Comparator;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;

public class ProductSizeProvider implements DataProvider {

    private TableColumn _fileSizeColumn;
    private final Comparator _productSizeComparator = new ProductSizeComparator();

    public boolean mustCreateData(final RepositoryEntry entry,final  Repository repository) {
        return false;
    }

    public void createData(final RepositoryEntry entry,final  Repository repository)
            throws IOException {
    }

    public Object getData(final RepositoryEntry entry,final  Repository repository)
            throws IOException {
        return new Float(entry.getProductSize());
    }

    public Comparator getComparator() {
        return _productSizeComparator;
    }

    public void cleanUp(final RepositoryEntry entry,final  Repository repository) {
    }

    public TableColumn getTableColumn() {
        if (_fileSizeColumn == null) {
            _fileSizeColumn = new TableColumn();
            _fileSizeColumn.setHeaderValue("File Size");
            _fileSizeColumn.setPreferredWidth(70);
            _fileSizeColumn.setResizable(true);
            _fileSizeColumn.setCellRenderer(new FileSizeCellRenderer());
        }
        return _fileSizeColumn;
    }

    private static class FileSizeCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            final JLabel jlabel = (JLabel) super
                    .getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                                   row, column);

            jlabel.setFont(jlabel.getFont().deriveFont(Font.BOLD));
            jlabel.setHorizontalAlignment(JLabel.CENTER);
            setText(String.format("%1$.2f MB", new Object[] {value }));
            return jlabel;
        }
    }

    private static class ProductSizeComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if(o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if(o2 == null) {
                return 1;
            }

            final Float f1 = (Float) o1;
            final Float f2 = (Float) o2;

            return f1.compareTo(f2);
        }
    }
}
