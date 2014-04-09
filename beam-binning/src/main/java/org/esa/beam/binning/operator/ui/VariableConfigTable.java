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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyAccessorFactory;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.jidesoft.grid.StringCellEditor;
import org.apache.commons.lang.ArrayUtils;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class VariableConfigTable {

    private static final String PROPERTY_SOURCE_TYPE = "sourceType";

    private final JTable table;
    private final VariableTableModel tableModel;
    private final JScrollPane scrollPane;
    private final BinningFormModel binningFormModel;
    private final AppContext appContext;

    private final Property currentSourceType;

    VariableConfigTable(final BinningFormModel binningFormModel, AppContext appContext) {
        this.binningFormModel = binningFormModel;
        this.appContext = appContext;
        this.currentSourceType = Property.create(PROPERTY_SOURCE_TYPE, Integer.class);
        currentSourceType.setContainer(new PropertyContainer());
        setCurrentSourceType(TargetVariableSpec.Source.RASTER_SOURCE_TYPE);

        tableModel = new VariableTableModel();
        tableModel.setColumnIdentifiers(new String[]{
                "Band / Expression",
                "Target name",
                "Aggregation"
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

        StringCellEditor editor = new StringCellEditor();
        table.setCellEditor(editor);
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int selectedRow = e.getFirstRow();
                if (selectedRow < 0) {
                    return;
                }
                TargetVariableSpec spec = tableModel.getSpec(selectedRow);
                if (spec == null) {
                    // spec has been deleted, do nothing.
                    return;
                }
                spec.source.type = currentSourceType.getValue();
                Object value = table.getValueAt(selectedRow, 0);
                String source = "";
                if (value != null) {
                    source = value.toString();
                }
                if (spec.source.type == TargetVariableSpec.Source.RASTER_SOURCE_TYPE) {
                    spec.source.bandName = source;
                } else {
                    spec.source.expression = source;
                }
                spec.targetName = table.getValueAt(selectedRow, 1).toString();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        table.updateUI();
                    }
                });
            }
        });

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                int column = table.columnAtPoint(e.getPoint());
                if (column == 0) {
                    JPopupMenu viewPopup = new JPopupMenu();
                    viewPopup.setBorderPainted(false);
                    final EditBandAction editBandAction = new EditBandAction();
                    binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
                                editBandAction.updateEnablement();
                            }
                        }
                    });
                    viewPopup.add(editBandAction);
                    viewPopup.add(new EditExpressionAction());
                    viewPopup.show(table, 1, table.rowAtPoint(e.getPoint()) * table.getRowHeight() + 1);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                int column = table.columnAtPoint(e.getPoint());
                if (column == 2) {
                    openEditAggregationDialog(table);
                }
            }
        });

        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        scrollPane = new JScrollPane(table);
    }

    static void removeProperties(PropertyContainer propertyContainer, String... properties) {
        for (String property : properties) {
            Property propertyToRemove = propertyContainer.getProperty(property);
            if (propertyToRemove != null) {
                propertyContainer.removeProperty(propertyToRemove);
            }
        }
    }

    static String createAggregationString(AggregatorDescriptor aggregatorDescriptor, PropertyContainer aggregatorProperties) {
        StringBuilder aggregationString = new StringBuilder(aggregatorDescriptor.getName());
        final Property[] properties = aggregatorProperties.getProperties();
        if (properties.length > 0) {
            aggregationString.append("(");
            for (int i = 0; i < properties.length; i++) {
                final Property property = properties[i];
                aggregationString
                        .append(property.getName())
                        .append("=")
                        .append(getValue(property));
                if (i < properties.length - 1) {
                    aggregationString.append(",");
                }
            }

            aggregationString.append(")");
        }
        return aggregationString.toString();
    }

    private static String getValue(Property property) {
        String value = "";
        if (property.getType().equals(Boolean.class) && property.getValue() == null) {
            value = "false";
        } else if (property.getValue() != null) {
            if (property.getType().isArray()) {
                Object[] values = property.getValue();
                for (int i = 0; i < values.length; i++) {
                    final Object o = values[i];
                    value += o;
                    if (i < values.length - 1) {
                        value += ",";
                    }
                }
                return value;
            } else {
                value = property.getValue().toString();
            }
        }
        return value;
    }

    private void setCurrentSourceType(int type) {
        try {
            currentSourceType.setValue(type);
        } catch (ValidationException e) {
            throw new IllegalStateException("Should never come here", e);
        }
    }

    static PropertyContainer createAggregatorProperties(AggregatorDescriptor selectedAggregatorDescriptor) {
        AggregatorConfig aggregatorConfig = selectedAggregatorDescriptor.createConfig();
        return PropertyContainer.createForFields(aggregatorConfig.getClass(),
                                                 new ParameterDescriptorFactory(),
                                                 new ClassFieldAccessorFactory(aggregatorConfig), true);
    }

    JComponent getComponent() {
        return scrollPane;
    }

    public void duplicateSelectedRow() {
        int rowIndex = table.getSelectedRows()[0];
        TargetVariableSpec spec = tableModel.getSpec(rowIndex);
        tableModel.setSpec(tableModel.getRowCount(), new TargetVariableSpec(spec));
        table.getSelectionModel().setSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
    }

    public void addNewRow() {
        tableModel.setSpec(tableModel.getRowCount(), new TargetVariableSpec());
    }

    public void removeSelectedRows() {
        if (table.getSelectedRows().length != 0) {
            int row = table.getSelectedRows()[0];
            tableModel.removeRow(row);
        }
    }

    public boolean canDuplicate() {
        int[] selectedRows = table.getSelectedRows();
        return tableModel.getRowCount() > 0 && selectedRows.length != 0;
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

    static List<AggregatorDescriptor> getAggregatorDescriptors(String... filterNames) {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        List<AggregatorDescriptor> allDescriptors = registry.getDescriptors(AggregatorDescriptor.class);
        final List<AggregatorDescriptor> filteredDescriptors = new ArrayList<>();
        for (final AggregatorDescriptor descriptor : allDescriptors) {
            for (String name : filterNames) {
                if (!descriptor.getName().equals(name)) {
                    filteredDescriptors.add(descriptor);
                }
            }
        }
        return filteredDescriptors;
    }

    private static void openEditAggregationDialog(JTable table) {
        int selectionIndex = table.getSelectionModel().getMinSelectionIndex();
        VariableTableModel model = (VariableTableModel) table.getModel();
        EditAggregationDialog editAggregationDialog = new EditAggregationDialog(UIUtils.getRootWindow(table), model.getSpec(selectionIndex));
        int result = editAggregationDialog.show();
        if (result == EditAggregationDialog.ID_OK) {
            TargetVariableSpec spec = editAggregationDialog.getSpec();
            model.setSpec(selectionIndex, spec);
        }
    }

    private static class ClassFieldAccessorFactory implements PropertyAccessorFactory {

        private final Object object;

        private ClassFieldAccessorFactory(Object object) {
            this.object = object;
        }

        @Override
        public PropertyAccessor createValueAccessor(Field field) {
            return new ClassFieldAccessor(object, field);
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
            List<TargetVariableSpec> specs = new ArrayList<>();
            for (TargetVariableSpec spec : tableModel.specs.values()) {
                if (StringUtils.isNotNullAndNotEmpty(spec.aggregationString)) {
                    specs.add(spec);
                }
            }
            return specs.toArray(new TargetVariableSpec[specs.size()]);
        }
    }

    private class EditBandAction extends AbstractAction {

        public EditBandAction() {
            super("Choose raster data...");
        }

        public void updateEnablement() {
            firePropertyChange("enabled", true, false); // old and new value are not important, they must only be different
        }

        @Override
        public boolean isEnabled() {
            return binningFormModel.getSourceProducts().length > 0;
        }

        @Override
        public void actionPerformed(ActionEvent ignored) {
            Product product = binningFormModel.getSourceProducts()[0];
            String[] bandNames = product.getBandNames();
            String[] tiePointGridNames = product.getTiePointGridNames();
            Object[] rasterNames = ArrayUtils.addAll(bandNames, tiePointGridNames);
            Object chosenRaster = JOptionPane.showInputDialog(appContext.getApplicationWindow(), "Choose raster data",
                                                              "Choose raster data", JOptionPane.PLAIN_MESSAGE, null,
                                                              rasterNames, rasterNames[0]);
            if (chosenRaster != null) {
                setCurrentSourceType(TargetVariableSpec.Source.RASTER_SOURCE_TYPE);
                table.setValueAt(chosenRaster.toString(), table.getSelectedRow(), table.getSelectedColumn());
            }
        }
    }

    private class EditExpressionAction extends AbstractAction {

        public EditExpressionAction() {
            super("Create expression...");
        }

        @Override
        public boolean isEnabled() {
            return binningFormModel.getSourceProducts().length > 0;
        }

        @Override
        public void actionPerformed(ActionEvent ignored) {
            String expression = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString();
            expression = editExpression(expression);
            if (expression != null) {
                setCurrentSourceType(TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE);
                table.setValueAt(expression, table.getSelectedRow(), table.getSelectedColumn());
            }
        }

        private String editExpression(String expression) {
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

    private class VariableTableModel implements TableModel {

        private final HashMap<Integer, TargetVariableSpec> specs = new HashMap<>();
        private final DefaultTableModel delegate = new DefaultTableModel();
        private final List<TableModelListener> listeners = new ArrayList<>();


        @Override
        public int getRowCount() {
            // null check is necessary, because specs indeed is null when method is called the first time
            //noinspection ConstantConditions
            if (specs == null) {
                return 0;
            }
            return specs.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return delegate.getColumnName(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TargetVariableSpec spec = specs.get(rowIndex);
            if (columnIndex == 0) {
                return spec.source.type == TargetVariableSpec.Source.RASTER_SOURCE_TYPE ?
                       spec.source.bandName : spec.source.expression;
            } else if (columnIndex == 1) {
                return spec.targetName;
            } else if (columnIndex == 2) {
                return spec.aggregationString;
            }
            return spec;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            TargetVariableSpec spec;
            if (!specs.containsKey(rowIndex)) {
                spec = new TargetVariableSpec();
                spec.source = new TargetVariableSpec.Source();
            } else {
                spec = specs.get(rowIndex);
            }

            String value = aValue.toString();
            if (columnIndex == 0) {
                spec.source.type = currentSourceType.getValue();
                if (spec.source.type == TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE) {
                    spec.source.expression = value;
                } else {
                    spec.source.bandName = value;
                }
            } else if (columnIndex == 1) {
                spec.targetName = value;
            } else if (columnIndex == 2) {
                spec.aggregationString = value;
            }
            specs.put(rowIndex, spec);
            notifyListeners(rowIndex);
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        public TargetVariableSpec getSpec(int selectedRow) {
            return specs.get(selectedRow);
        }

        public void setSpec(int selectedRow, TargetVariableSpec spec) {
            specs.put(selectedRow, spec);
            notifyListeners(selectedRow);
        }

        public void setColumnIdentifiers(String[] strings) {
            delegate.setColumnIdentifiers(strings);
        }

        public void removeRow(int row) {
            specs.remove(row);
            // go through all specs after row and subtract 1 from index
            List<Map.Entry<Integer, TargetVariableSpec>> newEntries = new ArrayList<>();
            List<Integer> keysToRemove = new ArrayList<>();
            for (Map.Entry<Integer, TargetVariableSpec> entry : specs.entrySet()) {
                if (entry.getKey() > row) {
                    newEntries.add(new AbstractMap.SimpleEntry<>(entry.getKey() - 1, entry.getValue()));
                    keysToRemove.add(entry.getKey());
                }
            }
            for (Integer key : keysToRemove) {
                specs.remove(key);
            }
            for (Map.Entry<Integer, TargetVariableSpec> newEntry : newEntries) {
                specs.put(newEntry.getKey(), newEntry.getValue());
            }
            notifyListeners();
        }

        private void notifyListeners() {
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this));
            }
        }

        private void notifyListeners(int selectedRow) {
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this, selectedRow));
            }
        }
    }
}
