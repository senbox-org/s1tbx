package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.MouseEventFilterFactory;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MosaicExpressionsPanel extends JPanel {

    private static final int PREFERRED_TABLE_WIDTH = 520;

    private final AppContext appContext;
    private final BindingContext bindingContext;

    private JTable variablesTable;
    private JTable conditionsTable;
    private MosaicFormModel mosaicModel;

    MosaicExpressionsPanel(AppContext appContext, MosaicFormModel model) {
        this.appContext = appContext;
        mosaicModel = model;
        this.bindingContext = new BindingContext(model.getPropertyContainer());
        init();
    }

    private void init() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);

        add(createVariablesPanel());
        add(createConditionsPanel());
    }

    private Component createVariablesPanel() {
        final String labelName = "Variables";  /*I18N*/

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTablePadding(3, 3);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setRowWeightY(0, 0.0);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder(labelName));
        panel.setName(labelName);

        panel.add(createVariablesButtonPanel(labelName));
        panel.add(createVariablesTable(labelName));

        return panel;
    }

    private JPanel createVariablesButtonPanel(String labelName) {
        final JPanel variableButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        variableButtonsPanel.setName(labelName);

        final Component bandFilterButton = createBandFilterButton();
        bandFilterButton.setName(labelName + "_bandFilter");
        variableButtonsPanel.add(bandFilterButton);

        final Component newVariableButton = createNewVariableButton();
        newVariableButton.setName(labelName + "_newVariable");
        variableButtonsPanel.add(newVariableButton);

        final Component removeVariableButton = createRemoveVariableButton();
        removeVariableButton.setName(labelName + "_removeVariable");
        variableButtonsPanel.add(removeVariableButton);

        final Component moveVariableUpButton = createMoveVariableUpButton();
        moveVariableUpButton.setName(labelName + "moveVariableUp");
        variableButtonsPanel.add(moveVariableUpButton);

        final Component moveVariableDownButton = createMoveVariableDownButton();
        moveVariableDownButton.setName(labelName + "moveVariableDown");
        variableButtonsPanel.add(moveVariableDownButton);
        mosaicModel.getPropertyContainer().addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = Boolean.FALSE.equals(evt.getNewValue());
                bandFilterButton.setEnabled(enabled);
                newVariableButton.setEnabled(enabled);
                removeVariableButton.setEnabled(enabled);
                moveVariableUpButton.setEnabled(enabled);
                moveVariableDownButton.setEnabled(enabled);

            }
        });
        return variableButtonsPanel;
    }

    private Component createConditionsPanel() {
        final String labelName = "Conditions";
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTablePadding(3, 3);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setRowWeightY(1, 1.0);
        final JPanel panel = new JPanel(layout);
        panel.setName(labelName);
        panel.setBorder(BorderFactory.createTitledBorder(labelName));
        final JPanel conditionsButtonsPanel = createConditionsButtonPanel(labelName);

        panel.add(conditionsButtonsPanel);
        panel.add(createConditionsTable(labelName));
        panel.add(createCombinePanel());
        return panel;
    }

    private JPanel createConditionsButtonPanel(String labelName) {
        final JPanel conditionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        conditionButtonsPanel.setName(labelName);

        final Component newConditionButton = createNewConditionButton();
        newConditionButton.setName(labelName + "_newCondition");
        conditionButtonsPanel.add(newConditionButton);

        final Component removeConditionButton = createRemoveConditionButton();
        removeConditionButton.setName(labelName + "_removeCondition");
        conditionButtonsPanel.add(removeConditionButton);

        final Component moveConditionUpButton = createMoveConditionUpButton();
        moveConditionUpButton.setName(labelName + "moveConditionUp");
        conditionButtonsPanel.add(moveConditionUpButton);

        final Component moveConditionDownButton = createMoveConditionDownButton();
        moveConditionDownButton.setName(labelName + "moveConditionDown");
        conditionButtonsPanel.add(moveConditionDownButton);

        mosaicModel.getPropertyContainer().addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = Boolean.FALSE.equals(evt.getNewValue());
                newConditionButton.setEnabled(enabled);
                removeConditionButton.setEnabled(enabled);
                moveConditionUpButton.setEnabled(enabled);
                moveConditionDownButton.setEnabled(enabled);
            }
        });

        return conditionButtonsPanel;
    }

    private JPanel createCombinePanel() {
        final JPanel combinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JComboBox combineComboBox = new JComboBox();
        bindingContext.bind("combine", combineComboBox);
        bindingContext.bindEnabledState("combine", false, "updateMode", true);
        final String displayName = bindingContext.getPropertySet().getDescriptor("combine").getDisplayName();
        combinePanel.add(new JLabel(displayName + ":"));
        combinePanel.add(combineComboBox);
        return combinePanel;
    }

    private Component createNewConditionButton() {
        AbstractButton newConditionsButton = createButton("icons/Plus24.gif", "newCondition");
        newConditionsButton.setToolTipText("Add new processing condition"); /*I18N*/
        newConditionsButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final int rows = conditionsTable.getRowCount();
                addRow(conditionsTable, new Object[]{"condition_" + rows, "", false}); /*I18N*/
            }
        });
        return newConditionsButton;
    }

    private Component createRemoveConditionButton() {
        AbstractButton removeConditionButton = createButton("icons/Minus24.gif", "removeCondition");
        removeConditionButton.setToolTipText("Remove selected rows."); /*I18N*/
        removeConditionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeRows(conditionsTable, conditionsTable.getSelectedRows());
            }
        });
        return removeConditionButton;
    }

    private Component createMoveConditionUpButton() {
        AbstractButton moveConditionUpButton = createButton("icons/MoveUp24.gif", "moveConditionUp");
        moveConditionUpButton.setToolTipText("Move up selected rows."); /*I18N*/
        moveConditionUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsUp(conditionsTable, conditionsTable.getSelectedRows());
            }
        });
        return moveConditionUpButton;
    }

    private Component createMoveConditionDownButton() {
        AbstractButton moveConditionDownButton = createButton("icons/MoveDown24.gif", "moveConditionDown");
        moveConditionDownButton.setToolTipText("Move down selected rows."); /*I18N*/
        moveConditionDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsDown(conditionsTable, conditionsTable.getSelectedRows());
            }
        });
        return moveConditionDownButton;
    }

    private JScrollPane createConditionsTable(final String labelName) {
        conditionsTable = new JTable() {
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
        conditionsTable.setName(labelName);
        conditionsTable.setRowSelectionAllowed(true);
        bindingContext.bind("conditions", new ConditionsTableAdapter(conditionsTable));
        bindingContext.bindEnabledState("conditions", false, "updateMode", true);
        conditionsTable.addMouseListener(createExpressionEditorMouseListener(conditionsTable, true));

        final JTableHeader tableHeader = conditionsTable.getTableHeader();
        tableHeader.setName(labelName);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        final TableColumnModel columnModel = conditionsTable.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);

        final TableColumn nameColumn = columnModel.getColumn(0);
        nameColumn.setPreferredWidth(100);
        nameColumn.setCellRenderer(new TCR());

        final TableColumn expressionColumn = columnModel.getColumn(1);
        expressionColumn.setPreferredWidth(360);
        expressionColumn.setCellRenderer(new TCR());
        final ExprEditor cellEditor = new ExprEditor(true);
        expressionColumn.setCellEditor(cellEditor);
        mosaicModel.getPropertyContainer().addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = Boolean.FALSE.equals(evt.getNewValue());
                cellEditor.button.setEnabled(enabled);                
            }
        });


        final TableColumn outputColumn = columnModel.getColumn(2);
        outputColumn.setPreferredWidth(40);

        final JScrollPane pane = new JScrollPane(conditionsTable);
        pane.setName(labelName);
        pane.setPreferredSize(new Dimension(PREFERRED_TABLE_WIDTH, 80));

        return pane;
    }

    private Component createBandFilterButton() {
        AbstractButton variableFilterButton = createButton("icons/Copy16.gif", "bandButton");
        variableFilterButton.setToolTipText("Choose the bands to process"); /*I18N*/
        variableFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Product product;
                try {
                    product = mosaicModel.getReferenceProduct();
                } catch (IOException ioe) {
                    appContext.handleError(ioe.getMessage(), ioe);
                    return;
                }
                if (product != null) {
                    final String[] availableBandNames = product.getBandNames();
                    final Band[] allBands = product.getBands();
                    final List dataVector = ((DefaultTableModel) variablesTable.getModel()).getDataVector();
                    final List<Band> existingBands = new ArrayList<Band>(dataVector.size());
                    for (Object aDataVector : dataVector) {
                        List row = (List) aDataVector;
                        final String name = (String) row.get(0);
                        final String expression = (String) row.get(1);
                        if (name == null || expression == null
                            || !StringUtils.contains(availableBandNames, name.trim())
                            || !name.trim().equals(expression.trim())) {
                            continue;
                        }
                        existingBands.add(product.getBand(name.trim()));
                    }
                    final BandChooser bandChooser = new BandChooser(appContext.getApplicationWindow(), "Band Chooser",
                                                                    null,
                                                                    allBands, /*I18N*/
                                                                    existingBands.toArray(
                                                                            new Band[existingBands.size()]));
                    if (bandChooser.show() == ModalDialog.ID_OK) {
                        final Band[] selectedBands = bandChooser.getSelectedBands();
                        for (Band selectedBand : selectedBands) {
                            if (!existingBands.contains(selectedBand)) {
                                final String name = selectedBand.getName();
                                addRow(variablesTable, new Object[]{name, name});
                            } else {
                                existingBands.remove(selectedBand);
                            }
                        }
                        final int[] rowsToRemove = new int[0];
                        final List newDataVector = ((DefaultTableModel) variablesTable.getModel()).getDataVector();
                        for (Band existingBand : existingBands) {
                            String bandName = existingBand.getName();
                            final int rowIndex = getBandRow(newDataVector, bandName);
                            if (rowIndex > -1) {
                                ArrayUtils.addToArray(rowsToRemove, rowIndex);
                            }
                        }
                        removeRows(variablesTable, rowsToRemove);
                    }
                }
            }
        });
        return variableFilterButton;
    }

    private static int getBandRow(List newDataVector, String bandName) {
        for (int i = 0; i < newDataVector.size(); i++) {
            List row = (List) newDataVector.get(i);
            if (bandName.equals(row.get(0)) && bandName.equals(row.get(1))) {
                return i;
            }
        }
        return -1;
    }

    private Component createNewVariableButton() {
        AbstractButton newVariableButton = createButton("icons/Plus24.gif", "newVariable");
        newVariableButton.setToolTipText("Add new processing variable"); /*I18N*/
        newVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final int rows = variablesTable.getRowCount();
                addRow(variablesTable, new Object[]{"variable_" + rows, ""}); /*I18N*/
            }
        });
        return newVariableButton;
    }

    private Component createRemoveVariableButton() {
        AbstractButton removeVariableButton = createButton("icons/Minus24.gif", "removeVariable");
        removeVariableButton.setToolTipText("Remove selected rows."); /*I18N*/
        removeVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeRows(variablesTable, variablesTable.getSelectedRows());
            }
        });
        return removeVariableButton;
    }

    private Component createMoveVariableUpButton() {
        AbstractButton moveVariableUpButton = createButton("icons/MoveUp24.gif", "moveVariableUp");
        moveVariableUpButton.setToolTipText("Move up selected rows."); /*I18N*/
        moveVariableUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsUp(variablesTable, variablesTable.getSelectedRows());
            }
        });
        return moveVariableUpButton;
    }

    private Component createMoveVariableDownButton() {
        AbstractButton moveVariableDownButton = createButton("icons/MoveDown24.gif", "moveVariableDown");
        moveVariableDownButton.setToolTipText("Move down selected rows."); /*I18N*/
        moveVariableDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsDown(variablesTable, variablesTable.getSelectedRows());
            }
        });
        return moveVariableDownButton;
    }

    private JScrollPane createVariablesTable(final String labelName) {
        variablesTable = new JTable();
        variablesTable.setName(labelName);
        variablesTable.setRowSelectionAllowed(true);
        bindingContext.bind("variables", new VariablesTableAdapter(variablesTable));
        bindingContext.bindEnabledState("variables", false, "updateMode", true);
        variablesTable.addMouseListener(createExpressionEditorMouseListener(variablesTable, false));

        final JTableHeader tableHeader = variablesTable.getTableHeader();
        tableHeader.setName(labelName);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        final TableColumnModel columnModel = variablesTable.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);

        final TableColumn nameColumn = columnModel.getColumn(0);
        nameColumn.setPreferredWidth(100);
        nameColumn.setCellRenderer(new TCR());

        final TableColumn expressionColumn = columnModel.getColumn(1);
        expressionColumn.setPreferredWidth(400);
        expressionColumn.setCellRenderer(new TCR());
        final ExprEditor exprEditor = new ExprEditor(false);
        expressionColumn.setCellEditor(exprEditor);
        mosaicModel.getPropertyContainer().addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = Boolean.FALSE.equals(evt.getNewValue());
                exprEditor.button.setEnabled(enabled);
            }
        });

        final JScrollPane scrollPane = new JScrollPane(variablesTable);
        scrollPane.setName(labelName);
        scrollPane.setPreferredSize(new Dimension(PREFERRED_TABLE_WIDTH, 150));

        return scrollPane;
    }

    private static AbstractButton createButton(final String path, String name) {
        final AbstractButton button = ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
        button.setName(name);
        return button;
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
        try {
            product = mosaicModel.getReferenceProduct();
        } catch (IOException ioe) {
            appContext.handleError(ioe.getMessage(), ioe);
            return 0;
        }
        if(product == null) {
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
         *
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

    private static void moveRowsDown(final JTable table, final int[] rows) {
        final int maxRow = table.getRowCount() - 1;
        for (int row1 : rows) {
            if (row1 == maxRow) {
                return;
            }
        }
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row + 1);
            rows[i]++;
        }
        selectRows(table, rows);
    }

    private static void moveRowsUp(final JTable table, final int[] rows) {
        for (int row1 : rows) {
            if (row1 == 0) {
                return;
            }
        }
        table.removeEditor();
        for (int row : rows) {
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row - 1);
        }
        selectRows(table, rows);
    }

    private static void removeRows(final JTable table, final int[] rows) {
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
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

    private static class TCR extends JLabel implements TableCellRenderer {

        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

        /**
         * Creates a <code>JLabel</code> instance with no image and with an empty string for the title. The label is
         * centered vertically in its display area. The label's contents, once set, will be displayed on the leading
         * edge of the label's display area.
         */
        private TCR() {
            setOpaque(true);
            setBorder(noFocusBorder);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            final boolean enabled = table.isEnabled();
            setText((String) value);

            if (isSelected) {
                super.setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else if (!enabled) {
                super.setForeground(UIManager.getColor("TextField.inactiveForeground"));
                super.setBackground(table.getBackground());
            } else {
                super.setForeground(table.getForeground());
                super.setBackground(table.getBackground());
            }

            setFont(table.getFont());

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
                if (table.isCellEditable(row, column)) {
                    super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                    super.setBackground(UIManager.getColor("Table.focusCellBackground"));
                }
            } else {
                setBorder(noFocusBorder);
            }

            setValue(value);

            return this;
        }

        private void setValue(Object value) {
            setText(value == null ? "" : value.toString());
        }
    }
}
