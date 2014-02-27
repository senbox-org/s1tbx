/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.ui.application.ToolView;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
abstract class TablePagePanel extends PagePanel {

    private final TableModel emptyTableModel;
    private final JTable table;

    TablePagePanel(ToolView parentDialog, String helpId, String titlePrefix, String defaultInformationText) {
        super(parentDialog, helpId, titlePrefix);
        emptyTableModel = new DefaultTableModel(1, 1);
        emptyTableModel.setValueAt(defaultInformationText, 0, 0);
        table = new JTable(emptyTableModel);
    }

    /**
     * Notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (event.getSourceNode() == getRaster() || event.getSourceNode() == getProduct()) {
            updateComponents();
        }
    }

    protected JTable getTable() {
        return table;
    }

    protected void showNoInformationAvailableMessage() {
        table.setModel(emptyTableModel);
    }

    protected void setCellRenderer(int column, TableCellRenderer renderer) {
        getTable().getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    static class AlternatingRowsRenderer extends DefaultTableCellRenderer {

        private Color brightBackground;
        private Color mediumBackground;

        public AlternatingRowsRenderer() {
            brightBackground = Color.white;
            mediumBackground = new Color((14 * brightBackground.getRed()) / 15,
                                         (14 * brightBackground.getGreen()) / 15,
                                         (14 * brightBackground.getBlue()) / 15);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBackground(getBackground(row));
            return label;
        }

        private Color getBackground(int row) {
            if (row % 2 == 0) {
                return mediumBackground;
            } else {
                return brightBackground;
            }
        }
    }

    static class TooltipAwareRenderer extends AlternatingRowsRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setToolTipText(label.getText());
            return label;
        }
    }

    static interface TableRow {
        int getColspan(int columnIndex, TableModel model);
    }

    protected static class EmptyTableRow implements TableRow {

        @Override
        public String toString() {
            return "\n";
        }

        @Override
        public int getColspan(int columnIndex, TableModel model) {
            return model.getColumnCount();
        }
    }

    protected static class SingleInformationRow implements TableRow {

        private final String content;

        public SingleInformationRow(String content) {
            this.content = content;
        }

        @Override
        public int getColspan(int columnIndex, TableModel model) {
            return model.getColumnCount();
        }

        @Override
        public String toString() {
            return content;
        }
    }

    protected static abstract class TablePagePanelModel implements TableModel {

        private List<TableModelListener> listeners = new ArrayList<>();
        protected List<TableRow> rows = new ArrayList<>();

        public void addRow(TableRow row) {
            rows.add(row);
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this, rows.size() - 1));
            }
        }

        public void clear() {
            rows.clear();
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this));
            }
        }

        @Override
        public void addTableModelListener(TableModelListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTableModelListener(TableModelListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void setValueAt(Object invalid1, int invalid2, int invalid3) {
            throw new IllegalStateException("Table must be non-editable!");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

    }

}
