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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class VariableConfigTable {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;
    private final BinningFormModel binningFormModel;
    private final AppContext appContext;
    private final HashMap<Integer, TargetVariableSpec> specs = new HashMap<>();

    VariableConfigTable(final BinningFormModel binningFormModel, AppContext appContext) {
        this.binningFormModel = binningFormModel;
        this.appContext = appContext;

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        tableModel.setColumnIdentifiers(new String[]{
                "Target prefix",
                "Band / Expression",
                "Aggregation",
                ""
        });

        tableModel.addTableModelListener(new VariableConfigTableListener());

        table = new JTable(tableModel) {
            @Override
            public Class getColumnClass(int column) {
                return String.class;
            }
        };
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setMinWidth(80);
        table.getColumnModel().getColumn(0).setWidth(80);

        table.getColumnModel().getColumn(1).setMinWidth(110);
        table.getColumnModel().getColumn(1).setWidth(220);

        table.getColumnModel().getColumn(2).setMinWidth(110);

        table.getColumnModel().getColumn(3).setMinWidth(40);
        table.getColumnModel().getColumn(3).setMaxWidth(40);
        table.getColumnModel().getColumn(3).setWidth(40);

        ButtonEditor buttonEditor = new ButtonEditor(table, specs, binningFormModel, appContext);

        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(buttonEditor);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        scrollPane = new JScrollPane(table);
    }

    JComponent getComponent() {
        return scrollPane;
    }

    public void duplicateSelectedRow() {
        int rowIndex = table.getSelectedRows()[0];
        TargetVariableSpec spec = specs.get(rowIndex);
        TargetVariableSpec copiedSpec = new TargetVariableSpec(spec);
        List<Map.Entry<Integer, TargetVariableSpec>> newEntries = new ArrayList<>();
        for (Map.Entry<Integer, TargetVariableSpec> entry : specs.entrySet()) {
            if (entry.getKey() > rowIndex) {
                newEntries.add(new AbstractMap.SimpleEntry<>(entry.getKey() + 1, entry.getValue()));
            }
        }
        for (Map.Entry<Integer, TargetVariableSpec> newEntry : newEntries) {
            specs.put(newEntry.getKey(), newEntry.getValue());
        }
        specs.put(rowIndex + 1, copiedSpec);
        for (int row = tableModel.getRowCount() - 1; row > rowIndex; row--) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                Object value = tableModel.getValueAt(row, col);
                tableModel.setValueAt(value, rowIndex + 1, col);
            }
        }
        String source =
                spec.source.type == TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE ? spec.source.expression :
                spec.source.bandName;
        tableModel.insertRow(rowIndex + 1, new Object[]{spec.targetPrefix, source, spec.aggregationString});
        table.getSelectionModel().setSelectionInterval(rowIndex + 1, rowIndex + 1);
    }

    public void addNewRow() {
        tableModel.addRow(new Object[]{"", "", ""});
    }

    public void removeSelectedRows() {
        if (table.getSelectedRows().length != 0) {
            tableModel.removeRow(table.getSelectedRows()[0]);
        }
    }

    public boolean canDuplicate() {
        int[] selectedRows = table.getSelectedRows();
        return tableModel.getRowCount() > 0 && selectedRows.length != 0 && specs.get(selectedRows[0]) != null;
    }

    public void addSelectionListener(final SelectionChangeListener listener) {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                listener.selectionChanged(new SelectionChangeEvent(table, null, null));
            }
        });
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                listener.selectionChanged(new SelectionChangeEvent(table, null, null));
            }
        });
    }

    private class VariableConfigTableListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent event) {
            try {
                binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_VARIABLE_CONFIGS, getSpecsAsArray());
            } catch (ValidationException e) {
                appContext.handleError("Unable to validate variable configurations.", e);
            }
        }

        private TargetVariableSpec[] getSpecsAsArray() {
            TargetVariableSpec[] targetVariableSpecs = new TargetVariableSpec[specs.size()];
            int i = 0;
            for (TargetVariableSpec spec : specs.values()) {
                targetVariableSpecs[i++] = spec;
            }
            return targetVariableSpecs;
        }
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            super("...");
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private static class ButtonEditor extends DefaultCellEditor {

        protected JButton button;

        public ButtonEditor(final JTable table, final HashMap<Integer, TargetVariableSpec> specs, final BinningFormModel binningFormModel, final AppContext appContext) {
            super(new JCheckBox());
            button = new JButton("...");
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    int selectionIndex = table.getSelectionModel().getMinSelectionIndex();
                    EditTargetVariableDialog editTargetVariableDialog = new EditTargetVariableDialog(UIUtils.getRootWindow(table), specs.get(selectionIndex), binningFormModel, appContext);
                    int result = editTargetVariableDialog.show();
                    if (result == EditTargetVariableDialog.ID_OK) {
                        TargetVariableSpec spec = editTargetVariableDialog.getSpec();
                        specs.put(selectionIndex, spec);
                        if (spec.source.type == TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE) {
                            table.setValueAt(spec.targetPrefix, selectionIndex, 0);
                        } else {
                            table.setValueAt("", selectionIndex, 0);
                        }
                        table.setValueAt(getSource(spec.source), selectionIndex, 1);
                        table.setValueAt(spec.aggregationString, selectionIndex, 2);
                    }
                    fireEditingStopped();
                }

                private String getSource(TargetVariableSpec.Source source) {
                    if (source.type == TargetVariableSpec.Source.RASTER_SOURCE_TYPE) {
                        return source.bandName;
                    } else if (source.type == TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE) {
                        return source.expression;
                    }
                    throw new IllegalStateException(
                            "Invalid source type, must be "
                            + TargetVariableSpec.Source.RASTER_SOURCE_TYPE + " or " +
                            TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE
                    );
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            return button;
        }

        public Object getCellEditorValue() {
            return super.getCellEditorValue();
        }
    }

}
