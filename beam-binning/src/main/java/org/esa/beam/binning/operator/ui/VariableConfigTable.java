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
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

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
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setMinWidth(100);
        table.getColumnModel().getColumn(0).setWidth(100);

        table.getColumnModel().getColumn(1).setMinWidth(180);
        table.getColumnModel().getColumn(1).setWidth(180);

        table.getColumnModel().getColumn(3).setMinWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setWidth(50);

        ButtonEditor buttonEditor = new ButtonEditor(table, specs, binningFormModel, appContext);

        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(buttonEditor);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        scrollPane = new JScrollPane(table);
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
                            TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE);
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

    JComponent getComponent() {
        return scrollPane;
    }

    public void addNewRow() {
        tableModel.addRow(new Object[]{"", "", ""});
    }

    public void removeSelectedRows() {
        while (table.getSelectedRows().length != 0) {
            tableModel.removeRow(table.getSelectedRows()[0]);
        }
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
}
