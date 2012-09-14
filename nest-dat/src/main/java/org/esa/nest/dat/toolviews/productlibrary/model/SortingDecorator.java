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
package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.util.Guardian;
import org.esa.nest.dat.toolviews.productlibrary.model.dataprovider.DataProvider;
import org.esa.nest.db.ProductEntry;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SortingDecorator extends AbstractTableModel {


    private static final int DESCENDING = -1;
    private static final int NOT_SORTED = 0;
    private static final int ASCENDING = 1;

    private static final Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

    private final ProductEntryTableModel _tableModel;
    private final JTableHeader _tableHeader;

    private final List<Directive> sortingColumns = new ArrayList<Directive>();
    private Row[] viewToModel = null;
    private boolean doSortBy = false;

    public enum SORT_BY { NAME, DATE, MISSON, TYPE, FILESIZE }
    private SORT_BY sortBy;
    private int sortedByDirection = NOT_SORTED;

    public SortingDecorator(final ProductEntryTableModel tableModel, final JTableHeader tableHeader) {
        Guardian.assertNotNull("tableModel", tableModel);
        Guardian.assertNotNull("tableHeader", tableHeader);

        _tableModel = tableModel;
        _tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(final TableModelEvent e) {
                initViewToModel();
                fireTableChanged(e);
            }
        });

        _tableHeader = tableHeader;
        _tableHeader.addMouseListener(new MouseHandler());
        _tableHeader.setDefaultRenderer(new SortableHeaderRenderer(_tableHeader.getDefaultRenderer()));
    }

    public int getRowCount() {
        return _tableModel.getRowCount();
    }

    public int getColumnCount() {
        return _tableModel.getColumnCount();
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return _tableModel.getValueAt(getSortedIndex(rowIndex), columnIndex);
    }

    @Override
    public String getColumnName(final int column) {
        return _tableModel.getColumnName(column);
    }

    @Override
    public Class getColumnClass(final int column) {
        return _tableModel.getColumnClass(column);
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return _tableModel.isCellEditable(getSortedIndex(row), column);
    }

    @Override
    public void setValueAt(final Object aValue, final int row, final int column) {
        _tableModel.setValueAt(aValue, getSortedIndex(row), column);
    }

    public int getSortedIndex(final int rowIndex) {
        final Row[] view = getViewToModel();
        if(rowIndex < view.length) {
            return view[rowIndex].modelIndex;
        } else if(view.length > 0) {
            return view[0].modelIndex;
        }
        return 0;
    }

    private Row[] getViewToModel() {
        if (viewToModel == null) {
            initViewToModel();
        }
        return viewToModel;
    }

    private void initViewToModel() {
        final int tableModelRowCount = _tableModel.getRowCount();
        viewToModel = new Row[tableModelRowCount];
        for (int row = 0; row < tableModelRowCount; row++) {
            viewToModel[row] = new Row(row);
        }
        if (isSorting()) {
            Arrays.sort(getViewToModel());
        }
    }

    boolean isSorting() {
        return !sortingColumns.isEmpty() || doSortBy;
    }

    public void sortBy(final SORT_BY val) {
        doSortBy = true;
        sortedByDirection = sortBy == val ? DESCENDING : ASCENDING;
        sortBy = val;
        initViewToModel();
        fireTableDataChanged();
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mouseClicked(final MouseEvent e) {
            final JTableHeader h = (JTableHeader) e.getSource();
            final TableColumnModel columnModel = h.getColumnModel();
            final int viewColumnIndex = columnModel.getColumnIndexAtX(e.getX());
            final int columnIndex = columnModel.getColumn(viewColumnIndex).getModelIndex();
            if (columnIndex != -1) {
                int direction = getSortingDirection(columnIndex);
                if (!e.isControlDown()) {
                    clearSortingDirections();
                    _tableHeader.repaint();
                }
                // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
                // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
                direction += (e.isShiftDown() ? -1 : 1);
                direction = (direction + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
                setDirectionForColumn(columnIndex, direction);
                doSortBy = false;
                initViewToModel();
                fireTableDataChanged();
            }
        }
    }

    private void setDirectionForColumn(final int column, final int direction) {
        final Directive directive = getDirective(column);
        if (directive != EMPTY_DIRECTIVE) {
            sortingColumns.remove(directive);
        }
        if (direction != NOT_SORTED) {
            sortingColumns.add(new Directive(column, direction));
        }
    }

    private class SortableHeaderRenderer implements TableCellRenderer {

        private final TableCellRenderer tableCellRenderer;

        public SortableHeaderRenderer(final TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            final Component c = tableCellRenderer.getTableCellRendererComponent(table,
                                                                                value, isSelected, hasFocus, row,
                                                                                column);
            if (c instanceof JLabel) {
                final JLabel l = (JLabel) c;
                l.setHorizontalTextPosition(JLabel.LEFT);
                final int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(getHeaderRendererIcon(modelColumn, (int) (l.getFont().getSize() * 1.6)));
            }
            return c;
        }
    }


    private Icon getHeaderRendererIcon(final int column, final int size) {
        final Directive directive = getDirective(column);
        if (directive == EMPTY_DIRECTIVE) {
            return null;
        }
        return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
    }

    private int getSortingDirection(final int column) {
        return getDirective(column).direction;
    }

    private void clearSortingDirections() {
        sortingColumns.clear();
    }

    private Directive getDirective(final int column) {
        for (Object sortingColumn : sortingColumns) {
            final Directive directive = (Directive) sortingColumn;
            if (directive.column == column) {
                return directive;
            }
        }
        return EMPTY_DIRECTIVE;
    }

    private static class Directive {

        private final int column;
        private final int direction;

        public Directive(final int column, final int direction) {
            this.column = column;
            this.direction = direction;
        }
    }

    private static class Arrow implements Icon {

        private final boolean descending;
        private final int size;
        private final int priority;

        public Arrow(final boolean descending, final int size, final int priority) {
            this.descending = descending;
            this.size = size;
            this.priority = priority;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, int y) {
            final Color color = c == null ? Color.GRAY : c.getBackground();
            // In a compound sort, make each succesive triangle 20%
            // smaller than the previous one.
            final int dx = (int) (size / 2 * Math.pow(0.8, priority));
            final int dy = descending ? dx : -dx;
            // Align icon (roughly) with font baseline.
            y = y + 5 * size / 6 + (descending ? -dy : 0);
            final int shift = descending ? 1 : -1;
            g.translate(x, y);

            // Right diagonal.
            g.setColor(color.darker());
            g.drawLine(dx / 2, dy, 0, 0);
            g.drawLine(dx / 2, dy + shift, 0, shift);

            // Left diagonal.
            g.setColor(color.brighter());
            g.drawLine(dx / 2, dy, dx, 0);
            g.drawLine(dx / 2, dy + shift, dx, shift);

            // Horizontal line.
            if (descending) {
                g.setColor(color.darker().darker());
            } else {
                g.setColor(color.brighter().brighter());
            }
            g.drawLine(dx, 0, 0, 0);

            g.setColor(color);
            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }

    private class Row implements Comparable {

        private final int modelIndex;

        public Row(final int index) {
            modelIndex = index;
        }

        public int compareTo(final Object o) {
            final int idxRow1 = modelIndex;
            final int idxRow2 = ((Row) o).modelIndex;

            if(doSortBy) {
                final Object o1 = _tableModel.getValueAt(idxRow1, 0);
                final Object o2 = _tableModel.getValueAt(idxRow2, 0);

                final int comparison = SORTEDBY_COMPARATOR.compare(o1, o2);
                if (comparison != 0) {
                    return sortedByDirection == DESCENDING ? -comparison : comparison;
                }
            }
            for (Object sortingColumn : sortingColumns) {
                final Directive directive = (Directive) sortingColumn;
                final int column = directive.column;
                final Object o1 = _tableModel.getValueAt(idxRow1, column);
                final Object o2 = _tableModel.getValueAt(idxRow2, column);

                final int comparison = getComparator(column).compare(o1, o2);
                if (comparison != 0) {
                    return directive.direction == DESCENDING ? -comparison : comparison;
                }
            }
            return 0;
        }
    }

    private static final Comparator COMPARABLE_COMAPRATOR = new Comparator() {
        public int compare(final Object o1, final Object o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else {
                return ((Comparable) o1).compareTo(o2);
            }
        }
    };

    private static final Comparator LEXICAL_COMPARATOR = new Comparator() {
        public int compare(final Object o1, final Object o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else {
                return o1.toString().compareTo(o2.toString());
            }
        }
    };

    private final Comparator SORTEDBY_COMPARATOR = new Comparator() {
        public int compare(final Object o1, final Object o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else if(o1 instanceof ProductEntry && o2 instanceof ProductEntry) {
                final ProductEntry e1 = (ProductEntry)o1;
                final ProductEntry e2 = (ProductEntry)o2;

                if(sortBy.equals(SORT_BY.NAME)) {
                    return e1.getName().compareTo(e2.getName());
                } else if(sortBy.equals(SORT_BY.TYPE)) {
                    return e1.getProductType().compareTo(e2.getProductType());
                } else if(sortBy.equals(SORT_BY.MISSON)) {
                    return e1.getMission().compareTo(e2.getMission());
                } else if(sortBy.equals(SORT_BY.DATE)) {
                    return e1.getFirstLineTime().getAsDate().compareTo(e2.getFirstLineTime().getAsDate());
                } else if(sortBy.equals(SORT_BY.FILESIZE)) {
                    final Long size1 = e1.getFileSize();
                    final Long size2 = e2.getFileSize();
                    return size1.compareTo(size2);
                }
            }
            return o1.toString().compareTo(o2.toString());
        }
    };

    private Comparator getComparator(final int column) {
        final DataProvider dataProvider = _tableModel.getDataProvider(column);

        final Class columnType = _tableModel.getColumnClass(column);
        Comparator comparator = dataProvider.getComparator();
        if (comparator == null) {
            if (Comparable.class.isAssignableFrom(columnType)) {
                comparator = COMPARABLE_COMAPRATOR;
            } else {
                comparator = LEXICAL_COMPARATOR;
            }
        }
        return comparator;
    }
}