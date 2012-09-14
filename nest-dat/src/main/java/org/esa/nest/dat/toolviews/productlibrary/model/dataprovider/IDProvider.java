/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.productlibrary.model.dataprovider;

import org.esa.nest.db.ProductEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Comparator;

public class IDProvider implements DataProvider {

    private final Comparator comparator = new IntComparator();
    private TableColumn column;

    public Comparator getComparator() {
        return comparator;
    }

    public TableColumn getTableColumn() {
        if(column == null) {
            try {
                column = new TableColumn();
                column.setHeaderValue("ID");
                column.setPreferredWidth(34);
                column.setResizable(true);
                column.setCellRenderer(new IDCellRenderer());
            } catch(Throwable e) {
                System.out.println("IDProvider: "+e.getMessage());
            }
        }
        return column;
    }

    private static class IDCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            try {
                final ProductEntry entry = (ProductEntry) value;
                if(entry != null) {
                    final String text = String.valueOf(entry.getId());

                    final JLabel jlabel = (JLabel) super
                            .getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

                    jlabel.setFont(jlabel.getFont().deriveFont(Font.BOLD));
                    jlabel.setToolTipText(entry.getFile().getAbsolutePath());
                    return jlabel;
                }
            } catch(Throwable e) {
                System.out.println("IDCellRenderer: "+e.getMessage());
            }
            return null;
        }
    }

    private static class IntComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if(o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if(o2 == null) {
                return 1;
            }

            final ProductEntry s1 = (ProductEntry) o1;
            final ProductEntry s2 = (ProductEntry) o2;

            if(s1.getId() < s2.getId())
                return -1;
            return 1;
        }
    }
}