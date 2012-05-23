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
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.AggregatorDescriptorRegistry;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Thomas Storm
 */
class VariableConfigTable {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;
    private final SortedSet<String> bandNames;
    private final String[] aggregatorNames;
    private final JComboBox bandNamesComboBox;
    private final BinningFormModel binningFormModel;
    private final AppContext appContext;

    VariableConfigTable(final BinningFormModel binningFormModel, AppContext appContext) {
        this.binningFormModel = binningFormModel;
        this.appContext = appContext;
        bandNames = new TreeSet<String>();
        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
                    updateBandNames();
                }
            }
        });
        final AggregatorDescriptor[] aggregatorDescriptors = AggregatorDescriptorRegistry.getInstance().getAggregatorDescriptors();
        aggregatorNames = new String[aggregatorDescriptors.length];
        for (int i = 0; i < aggregatorDescriptors.length; i++) {
            aggregatorNames[i] = aggregatorDescriptors[i].getName();
        }

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1 ||
                       table.getSelectedRow() == row && tableModel.getValueAt(row, 0).toString().equals("<expression>");
            }
        };
        tableModel.setColumnIdentifiers(new String[]{
                "Band",
                "Expression",
                "Aggregation",
                "Weight",
                "Fill value"
        });

        tableModel.addTableModelListener(new VariableConfigTableListener(this, this.binningFormModel));

        table = new JTable(tableModel) {
            @Override
            public Class getColumnClass(int column) {
                if (column == 3 || column == 4) {
                    return Double.class;
                } else {
                    return String.class;
                }
            }
        };
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setMinWidth(60);
        table.getColumnModel().getColumn(1).setMinWidth(60);
        table.getColumnModel().getColumn(2).setMinWidth(100);
        table.getColumnModel().getColumn(3).setMinWidth(60);
        table.getColumnModel().getColumn(4).setMinWidth(60);

        bandNamesComboBox = new JComboBox(bandNames.toArray());

        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(bandNamesComboBox));
        table.getColumnModel().getColumn(1).setCellEditor(new CellExpressionEditor());
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox(aggregatorNames)));
        final DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, value.toString(), isSelected, hasFocus, row, column);
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(cellRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(cellRenderer);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scrollPane = new JScrollPane(table);
    }

    JComponent getComponent() {
        return scrollPane;
    }

    void addRow(final String name, String expression, String algorithmName, double weightCoefficient, float fillValue) {
        if (algorithmName == null || !StringUtils.contains(aggregatorNames, algorithmName)) {
            algorithmName = AggregatorAverage.Descriptor.NAME;
        }
        tableModel.addRow(new Object[]{name, expression, algorithmName, weightCoefficient, fillValue});
    }

    void removeSelectedRows() {
        while (table.getSelectedRows().length != 0) {
            tableModel.removeRow(table.getSelectedRows()[0]);
        }
    }

    private Row[] getRows() {
        final List dataList = tableModel.getDataVector();
        final Row[] rows = new Row[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            final List dataListRow = (List) dataList.get(i);
            rows[i] = new Row((String) dataListRow.get(0),
                              (String) dataListRow.get(1),
                              (String) dataListRow.get(2),
                              (Double) dataListRow.get(3),
                              (Float) dataListRow.get(4));
        }
        return rows;
    }

    private void updateBandNames() {
        bandNames.clear();
        final Product[] sourceProducts = binningFormModel.getSourceProducts();
        for (Product sourceProduct : sourceProducts) {
            Collections.addAll(bandNames, sourceProduct.getBandNames());
        }
        bandNames.add("<expression>");
        updateBandNameCombobox();
    }

    private void updateBandNameCombobox() {
        ((DefaultComboBoxModel) bandNamesComboBox.getModel()).removeAllElements();
        for (String bandName : bandNames) {
            bandNamesComboBox.addItem(bandName);
        }
    }

    private class CellExpressionEditor extends AbstractCellEditor implements TableCellEditor {

        private final JPanel editorComponent;
        private final JTextField textField;

        CellExpressionEditor() {

            final JButton button = new JButton("...");
            final Dimension preferredSize = button.getPreferredSize();
            preferredSize.setSize(25, preferredSize.getHeight());
            button.setPreferredSize(preferredSize);
            button.setEnabled(false);

            final ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final String expression = editExpression(textField.getText());
                    if (expression != null) {
                        textField.setText(expression);
                        fireEditingStopped();
                    } else {
                        fireEditingCanceled();
                    }
                }
            };
            button.addActionListener(actionListener);
            binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
                        if (binningFormModel.getSourceProducts().length > 0) {
                            button.setEnabled(true);
                        } else {
                            button.setEnabled(false);
                        }
                    }
                }
            });
            textField = new JTextField();

            editorComponent = new JPanel(new BorderLayout());
            editorComponent.add(textField);
            editorComponent.add(button, BorderLayout.EAST);
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            textField.setText((String) value);
            return editorComponent;
        }

        private String editExpression(String expression) {
            if (binningFormModel.getSourceProducts().length == 0) {
                return null;
            }
            final Product product;
            product = binningFormModel.getSourceProducts()[0];
            final ProductExpressionPane expressionPane = ProductExpressionPane.createGeneralExpressionPane(
                    new Product[]{product}, product, appContext.getPreferences());
            expressionPane.setCode(expression);
            final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
            if (i == ModalDialog.ID_OK) {
                return expressionPane.getCode();
            }
            return null;
        }
    }

    private class VariableConfigTableListener implements TableModelListener {

        private VariableConfigTable bandsTable;

        private BinningFormModel binningFormModel;

        private VariableConfigTableListener(VariableConfigTable bandsTable, BinningFormModel binningFormModel) {
            this.bandsTable = bandsTable;
            this.binningFormModel = binningFormModel;
        }

        @Override
        public void tableChanged(TableModelEvent event) {
            final TableRow[] tableRows = new TableRow[bandsTable.getRows().length];
            final Row[] rows = bandsTable.getRows();
            for (int i = 0; i < rows.length; i++) {
                final Row row = rows[i];
                final AggregatorDescriptor aggregatorDescriptor = AggregatorDescriptorRegistry.getInstance().getAggregatorDescriptor(row.algorithmName);
                tableRows[i] = new TableRow(row.bandName,
                                            row.expression,
                                            aggregatorDescriptor,
                                            row.weightCoefficient,
                                            row.fillValue);
            }
            try {
                binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_VARIABLE_CONFIGS, tableRows);
            } catch (ValidationException e) {
                appContext.handleError("Unable to validate variable configurations.", e);
            }
        }
    }

    private static class Row {

        private final String bandName;
        private final String expression;
        private final String algorithmName;
        private final double weightCoefficient;
        private final float fillValue;

        Row(String bandName, String expression, String algorithmName, double weightCoefficient, float fillValue) {
            this.bandName = bandName;
            this.algorithmName = algorithmName;
            this.weightCoefficient = weightCoefficient;
            this.fillValue = fillValue;
            this.expression = expression;
        }
    }
}
