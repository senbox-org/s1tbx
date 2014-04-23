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
import com.bc.ceres.swing.Grid;
import com.bc.ceres.swing.ListControlBar;
import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.MouseEventFilterFactory;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * The panel in the binning operator UI which allows for specifying the configuration of binning variables.
 *
 * @author Norman
 */
class BinningVariablesPanel2 extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private double currentResolution;

    BinningVariablesPanel2(AppContext appContext, BinningFormModel binningFormModel) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        setLayout(new BorderLayout());
        add(createBandsPanel(), BorderLayout.CENTER);
        add(createParametersPanel(), BorderLayout.SOUTH);
    }

    private JComponent createBandsPanel() {
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.setProportionalLayout(true);
        splitPane.setShowGripper(true);
        splitPane.add(createAggregatorPanel());
        splitPane.add(createVariablePanel());
        splitPane.setProportions(new double[]{0.7});
        return splitPane;
    }

    private JPanel createParametersPanel() {
        JLabel validPixelExpressionLabel = new JLabel("Valid pixel expression:");
        final JButton validPixelExpressionButton = new JButton("...");
        final Dimension preferredSize = validPixelExpressionButton.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        validPixelExpressionButton.setPreferredSize(preferredSize);
        validPixelExpressionButton.setEnabled(hasSourceProducts());
        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)
                        || evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCT_PATHS)) {
                    validPixelExpressionButton.setEnabled(hasSourceProducts());
                }
            }
        });

        JLabel targetHeightLabel = new JLabel("#Rows (90N - 90S):");
        final JTextField validPixelExpressionField = new JTextField();
        validPixelExpressionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final String expression = editExpression(validPixelExpressionField.getText());
                if (expression != null) {
                    validPixelExpressionField.setText(expression);
                    try {
                        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_EXPRESSION, expression);
                    } catch (ValidationException e) {
                        appContext.handleError("Invalid expression", e);
                    }
                }
            }
        });

        final JTextField targetHeightTextField = new IntegerTextField(BinningFormModel.DEFAULT_NUM_ROWS);

        JLabel resolutionLabel = new JLabel("Spatial resolution (km/px):");
        final String defaultResolution = getString(computeResolution(BinningFormModel.DEFAULT_NUM_ROWS));
        final JTextField resolutionTextField = new DoubleTextField(defaultResolution);
        JButton resolutionButton = new JButton("default");

        JLabel supersamplingLabel = new JLabel("Supersampling:");
        final JTextField superSamplingTextField = new IntegerTextField(1);

        final ResolutionTextFieldListener listener = new ResolutionTextFieldListener(resolutionTextField, targetHeightTextField);

        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, Integer.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, Integer.class));

        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, targetHeightTextField);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, superSamplingTextField);

        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).setPropertyValue(BinningFormModel.DEFAULT_NUM_ROWS);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING).setPropertyValue(1);

        binningFormModel.getBindingContext().getPropertySet().getProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).addPropertyChangeListener(
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateResolutionLabel(targetHeightTextField, resolutionTextField, listener);
                    }
                }
        );

        resolutionTextField.addFocusListener(listener);
        resolutionTextField.addActionListener(listener);
        resolutionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resolutionTextField.setText(defaultResolution);
                listener.update();
            }
        });

        validPixelExpressionLabel.setToolTipText("Only those pixels matching this expression are considered");
        targetHeightLabel.setToolTipText("<html>The number of rows of the <b>maximum</b> target grid</html>");
        resolutionLabel.setToolTipText("The spatial resolution, directly depending on #rows");
        supersamplingLabel.setToolTipText("Every input pixel is subdivided into n x n sub-pixels in order to reduce or avoid the Moiré effect");

        TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableWeightX(0.0);
        layout.setCellColspan(1, 1, 2);
        layout.setCellColspan(3, 1, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(1, 1.0);
        layout.setTablePadding(10, 5);

        final JPanel parametersPanel = new JPanel(layout);

        parametersPanel.add(validPixelExpressionLabel);
        parametersPanel.add(validPixelExpressionField);
        parametersPanel.add(validPixelExpressionButton);

        parametersPanel.add(targetHeightLabel);
        parametersPanel.add(targetHeightTextField);

        parametersPanel.add(resolutionLabel);
        parametersPanel.add(resolutionTextField);
        parametersPanel.add(resolutionButton);

        parametersPanel.add(supersamplingLabel);
        parametersPanel.add(superSamplingTextField);

        return parametersPanel;
    }

    private static int computeNumRows(double resolution) {
        final double RE = 6378.145;
        int numRows = (int) ((RE * Math.PI) / resolution) + 1;
        return numRows % 2 == 0 ? numRows : numRows + 1;
    }

    private static double computeResolution(int numRows) {
        final double RE = 6378.145;
        return (RE * Math.PI) / (numRows - 1);
    }

    private static void updateResolutionLabel(JTextField targetHeightTextField, JTextField resolutionField, ResolutionTextFieldListener listener) {
        resolutionField.setText(getResolutionString(Integer.parseInt(targetHeightTextField.getText())));
        listener.update();
    }

    static String getResolutionString(int numRows) {
        double number = computeResolution(numRows);
        return getString(number);
    }

    private static String getString(double number) {
        final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        final DecimalFormat decimalFormat = new DecimalFormat("#.##", formatSymbols);
        return decimalFormat.format(number);
    }

    private boolean hasSourceProducts() {
        return binningFormModel.getSourceProducts().length > 0;
    }

    private String editExpression(String expression) {
        final Product product = binningFormModel.getSourceProducts()[0];
        if (product == null) {
            return null;
        }
        final ProductExpressionPane expressionPane;
        expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                           appContext.getPreferences());
        expressionPane.setCode(expression);
        final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return expressionPane.getCode();
        }
        return null;
    }

    private JPanel createVariablePanel() {
        JTable variableTable = createVariableTable("Variables (optional)");
        ListControlBar gridControlBar = ListControlBar.create(ListControlBar.HORIZONTAL, variableTable, new VariableController(variableTable));

        JScrollPane scrollPane = new JScrollPane(variableTable);
        scrollPane.setBorder(null);

        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBorder(new TitledBorder("Variables (optional)"));
        panel.add(gridControlBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JTable createVariableTable(final String labelName) {
        JTable variableTable = new JTable() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class getColumnClass(int column) {
                if (column == 2) {
                    return Boolean.class;
                } else {
                    return super.getColumnClass(column);
                }
            }
        };
        final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Name", "Expression"}, 0);
        tableModel.addTableModelListener(new VariableConfigTableListener(tableModel));
        variableTable.setModel(tableModel);
        variableTable.setName(labelName);
        variableTable.setRowSelectionAllowed(true);
        variableTable.addMouseListener(createExpressionEditorMouseListener(variableTable, true));

        final JTableHeader tableHeader = variableTable.getTableHeader();
        tableHeader.setName(labelName);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        final TableColumnModel columnModel = variableTable.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);

        final TableColumn nameColumn = columnModel.getColumn(0);
        nameColumn.setPreferredWidth(100);

        final TableColumn expressionColumn = columnModel.getColumn(1);
        expressionColumn.setPreferredWidth(360);
        final ExprEditor cellEditor = new ExprEditor(true);
        expressionColumn.setCellEditor(cellEditor);

        return variableTable;
    }

    private static class VariableController implements ListControlBar.ListController {

        final JTable table;

        private VariableController(JTable table) {
            this.table = table;
        }

        @Override
        public boolean addRow(int index) {
            final int rows = table.getRowCount();
            addRow(table, new Object[]{"variable_" + rows, ""}); /*I18N*/
            return true;
        }

        private static void addRow(final JTable table, final Object[] rowData) {
            table.removeEditor();
            ((DefaultTableModel) table.getModel()).addRow(rowData);
            final int row = table.getRowCount() - 1;
            final int numCols = table.getColumnModel().getColumnCount();
            for (int i = 0; i < Math.min(numCols, rowData.length); i++) {
                Object o = rowData[i];
                table.setValueAt(o, row, i);
            }
            selectRows(table, row, row);
        }

        @Override
        public boolean removeRows(int[] indices) {
            removeRows(table, table.getSelectedRows());
            return true;
        }

        private static void removeRows(final JTable table, final int[] rows) {
            table.removeEditor();
            for (int i = rows.length - 1; i > -1; i--) {
                int row = rows[i];
                ((DefaultTableModel) table.getModel()).removeRow(row);
            }
        }

        @Override
        public boolean moveRowUp(int index) {
            moveRowsUp(table, table.getSelectedRows());
            return true;
        }

        @Override
        public boolean moveRowDown(int index) {
            moveRowsDown(table, table.getSelectedRows());
            return true;
        }
    }


    private static void moveRowsDown(final JTable table, final int[] rows) {
        final int maxRow = table.getRowCount() - 1;
        for (int row1 : rows) {
            if (row1 == maxRow) {
                return;
            }
        }
        table.removeEditor();
        int[] selectedRows = rows.clone();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row + 1);
            selectedRows[i] = row + 1;
        }
        selectRows(table, selectedRows);
    }

    private static void moveRowsUp(final JTable table, final int[] rows) {
        for (int row1 : rows) {
            if (row1 == 0) {
                return;
            }
        }
        table.removeEditor();
        int[] selectedRows = rows.clone();
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row - 1);
            selectedRows[i] = row - 1;
        }
        selectRows(table, selectedRows);
    }

    private static void selectRows(final JTable table, final int[] rows) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : rows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }

    private static void selectRows(JTable table, int min, int max) {
        final int numRows = max + 1 - min;
        if (numRows <= 0) {
            return;
        }
        selectRows(table, prepareRows(numRows, min));
    }

    private static int[] prepareRows(final int numRows, int min) {
        final int[] rows = new int[numRows];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = min + i;
        }
        return rows;
    }

    private static JPanel createAggregatorPanel() {
        final Grid grid = new Grid(6, false);
        grid.getLayout().setTablePadding(4, 3);
        grid.getLayout().setTableAnchor(TableLayout.Anchor.BASELINE);
        grid.getLayout().setTableAnchor(TableLayout.Anchor.NORTHWEST);
        grid.getLayout().setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnFill(3, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnWeightX(2, 1.0);
        grid.getLayout().setColumnWeightX(3, 1.0);
        grid.getLayout().setColumnWeightX(4, 1.0);
        grid.setHeaderRow(
                /*1*/ new JLabel("<html><b>Agg.</b>"),
                /*2*/ new JLabel("<html><b>Source</b>"),
                /*3*/ new JLabel("<html><b>Targets</b>"),
                /*4*/ new JLabel("<html><b>Parameters</b>"),
                /*5*/ null
        );
        ListControlBar gridControlBar = ListControlBar.create(ListControlBar.HORIZONTAL, grid, new AggregatorTableController(grid));

        final JCheckBox sel = new JCheckBox();
        sel.setToolTipText("Show/hide selection column");
        sel.setBorderPaintedFlat(true);
        sel.setBorderPainted(false);
        sel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid.setShowSelectionColumn(sel.isSelected());
            }
        });
        gridControlBar.add(sel, 0);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        panel.add(gridControlBar, BorderLayout.SOUTH);
        return panel;
    }

    private MouseListener createExpressionEditorMouseListener(final JTable table, final boolean booleanExpected) {
        final MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int column = table.getSelectedColumn();
                    if (column == 1) {
                        table.removeEditor();
                        final int row = table.getSelectedRow();
                        final String[] value = new String[]{(String) table.getValueAt(row, column)};
                        final int i = editExpression(value, booleanExpected);
                        if (ModalDialog.ID_OK == i) {
                            table.setValueAt(value[0], row, column);
                        }
                    }
                }
            }
        };
        return MouseEventFilterFactory.createFilter(mouseListener);
    }

    private int editExpression(String[] value, final boolean booleanExpected) {
        Product product;
        product = binningFormModel.getSourceProducts()[0];
        if (product == null) {
            final String msg = "No source product specified.";
            appContext.handleError(msg, new IllegalStateException(msg));
            return 0;
        }
        final ProductExpressionPane pep;
        if (booleanExpected) {
            pep = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                    appContext.getPreferences());
        } else {
            pep = ProductExpressionPane.createGeneralExpressionPane(new Product[]{product}, product,
                                                                    appContext.getPreferences());
        }
        pep.setCode(value[0]);
        final int i = pep.showModalDialog(appContext.getApplicationWindow(), value[0]);
        if (i == ModalDialog.ID_OK) {
            value[0] = pep.getCode();
        }
        return i;
    }

    private static class IntegerTextField extends JTextField {

        private final static String disallowedChars = "`§~!@#$%^&*()_+=\\|\"':;?/>.<,- ";

        public IntegerTextField(int defaultValue) {
            super(defaultValue + "");
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

    private class DoubleTextField extends JTextField {

        private final static String disallowedChars = "`§~!@#$%^&*()_+=\\|\"':;?/><,- ";

        public DoubleTextField(String defaultValue) {
            super(defaultValue);
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

    private class ResolutionTextFieldListener extends FocusAdapter implements ActionListener {

        private final JTextField resolutionTextField;
        private final JTextField numRowsTextField;

        public ResolutionTextFieldListener(JTextField resolutionTextField, JTextField numRowsTextField) {
            this.resolutionTextField = resolutionTextField;
            this.numRowsTextField = numRowsTextField;
        }

        @Override
        public void focusLost(FocusEvent e) {
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
        }

        private void update() {
            double resolution = Double.parseDouble(resolutionTextField.getText());
            if (Math.abs(currentResolution - resolution) > 1E-6) {
                binningFormModel.getBindingContext().getPropertySet().setValue(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, computeNumRows(resolution));
                numRowsTextField.setText(String.valueOf(computeNumRows(resolution)));
                currentResolution = resolution;
            }
        }
    }

    private class ExprEditor extends AbstractCellEditor implements TableCellEditor {

        private final JButton button;
        private String[] value;

        private ExprEditor(final boolean booleanExpected) {
            button = new JButton("...");
            final Dimension preferredSize = button.getPreferredSize();
            preferredSize.setSize(25, preferredSize.getHeight());
            button.setPreferredSize(preferredSize);
            value = new String[1];
            final ActionListener actionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final int i = editExpression(value, booleanExpected);
                    if (i == ModalDialog.ID_OK) {
                        fireEditingStopped();
                    } else {
                        fireEditingCanceled();
                    }
                }
            };
            button.addActionListener(actionListener);
        }

        /**
         * Returns the value contained in the editor.
         *
         * @return the value contained in the editor
         */
        @Override
        public Object getCellEditorValue() {
            return value[0];
        }

        /**
         * Sets an initial <code>value</code> for the editor.  This will cause the editor to <code>stopEditing</code>
         * and lose any partially edited value if the editor is editing when this method is called. <p>
         * <p/>
         * Returns the component that should be added to the client's <code>Component</code> hierarchy.  Once installed
         * in the client's hierarchy this component will then be able to draw and receive user input.
         *
         * @param table      the <code>JTable</code> that is asking the editor to edit; can be <code>null</code>
         * @param value      the value of the cell to be edited; it is up to the specific editor to interpret and draw the
         *                   value.  For example, if value is the string "true", it could be rendered as a string or it could be rendered
         *                   as a check box that is checked.  <code>null</code> is a valid value
         * @param isSelected true if the cell is to be rendered with highlighting
         * @param row        the row of the cell being edited
         * @param column     the column of the cell being edited
         * @return the component for editing
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                     int column) {
            final JPanel renderPanel = new JPanel(new BorderLayout());
            final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
            final Component label = defaultRenderer.getTableCellRendererComponent(table, value, isSelected,
                                                                                  false, row, column);
            renderPanel.add(label);
            renderPanel.add(button, BorderLayout.EAST);
            this.value[0] = (String) value;
            return renderPanel;
        }
    }

    private class VariableConfigTableListener implements TableModelListener {

        private final TableModel tableModel;

        VariableConfigTableListener(TableModel tableModel) {
            this.tableModel = tableModel;
        }

        @Override
        public void tableChanged(TableModelEvent event) {
            try {
                binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_VARIABLE_CONFIGS, getVariableConfigs());
            } catch (ValidationException e) {
                appContext.handleError("Unable to validate variable configurations.", e);
            }
        }

        private VariableConfig[] getVariableConfigs() {
            final int rowCount = tableModel.getRowCount();
            VariableConfig[] variableConfigs = new VariableConfig[rowCount];
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String expression = (String) tableModel.getValueAt(i, 1);
                variableConfigs[i] = new VariableConfig(name, expression);
            }
            return variableConfigs;
        }
    }
}
