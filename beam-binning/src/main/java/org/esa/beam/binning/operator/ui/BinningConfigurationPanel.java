/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * The panel in the binning operator UI which allows for specifying the configuration of binning variables.
 *
 * @author Norman
 */
class BinningConfigurationPanel extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private double currentGridResolution;
    private AggregatorTableController aggregatorTableController;
    private VariableTableController variableTableController;

    BinningConfigurationPanel(AppContext appContext, BinningFormModel binningFormModel) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        setLayout(new BorderLayout());
        add(createAggregatorsAndVariablesPanel(), BorderLayout.CENTER);
        add(createParametersPanel(), BorderLayout.SOUTH);
    }

    VariableTableController getVariableTableController() {
        return variableTableController;
    }

    AggregatorTableController getAggregatorTableController() {
        return aggregatorTableController;
    }

    private JComponent createAggregatorsAndVariablesPanel() {
        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.add(createAggregatorsPanel());
        splitPane.add(createVariablesPanel());
        splitPane.setShowGripper(true);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.6});
        return splitPane;
    }

    private JPanel createParametersPanel() {
        BindingContext bindingContext = binningFormModel.getBindingContext();

        JLabel validPixelExpressionLabel = new JLabel("Valid pixel expression:");
        final JButton validPixelExpressionButton = new JButton("...");
        final Dimension preferredSize = validPixelExpressionButton.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        validPixelExpressionButton.setPreferredSize(preferredSize);
        validPixelExpressionButton.setEnabled(hasSourceProducts());
        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_CONTEXT_SOURCE_PRODUCT)) {
                    validPixelExpressionButton.setEnabled(hasSourceProducts());
                }
            }
        });

        JLabel targetHeightLabel = new JLabel("#Rows (90N - 90S):");
        final JTextField validPixelExpressionField = new JTextField();
        bindingContext.bind(BinningFormModel.PROPERTY_KEY_MASK_EXPR, validPixelExpressionField);
        validPixelExpressionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final String expression = editExpression(validPixelExpressionField.getText());
                if (expression != null) {
                    try {
                        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_MASK_EXPR, expression);
                    } catch (ValidationException e) {
                        appContext.handleError("Invalid expression", e);
                    }
                }
            }
        });

        final JTextField numRowsTextField = new IntegerTextField(BinningFormModel.DEFAULT_NUM_ROWS);

        JLabel resolutionLabel = new JLabel("Spatial resolution (km/px):");
        final String defaultResolution = getString(computeResolution(BinningFormModel.DEFAULT_NUM_ROWS));
        final JTextField resolutionTextField = new DoubleTextField(defaultResolution);
        JButton resolutionButton = new JButton("default");

        JLabel superSamplingLabel = new JLabel("Super-sampling:");
        final JTextField superSamplingTextField = new IntegerTextField(1);

        final ResolutionTextFieldListener listener = new ResolutionTextFieldListener(resolutionTextField, numRowsTextField);

        bindingContext.bind(BinningFormModel.PROPERTY_KEY_NUM_ROWS, numRowsTextField);
        bindingContext.bind(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, superSamplingTextField);

        bindingContext.getBinding(BinningFormModel.PROPERTY_KEY_NUM_ROWS).setPropertyValue(BinningFormModel.DEFAULT_NUM_ROWS);
        bindingContext.getBinding(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING).setPropertyValue(1);

        bindingContext.getPropertySet().getProperty(BinningFormModel.PROPERTY_KEY_NUM_ROWS).addPropertyChangeListener(
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateResolutionLabel(numRowsTextField, resolutionTextField, listener);
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
        superSamplingLabel.setToolTipText("Every input pixel is subdivided into n x n sub-pixels in order to reduce or avoid the Moiré effect");

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
        parametersPanel.add(numRowsTextField);

        parametersPanel.add(resolutionLabel);
        parametersPanel.add(resolutionTextField);
        parametersPanel.add(resolutionButton);

        parametersPanel.add(superSamplingLabel);
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
        return binningFormModel.getContextProduct() != null;
    }

    private String editExpression(String expression) {
        final Product product = binningFormModel.getContextProduct();
        if (product == null) {
            return null;
        }
        final ProductExpressionPane expressionPane;
        expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product, appContext.getPreferences());
        expressionPane.setCode(expression);
        final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return expressionPane.getCode();
        }
        return null;
    }

    private JPanel createAggregatorsPanel() {
        final Grid grid = new Grid(6, false);
        TableLayout gridLayout = grid.getLayout();
        gridLayout.setTablePadding(4, 3);
        gridLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        gridLayout.setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        gridLayout.setColumnFill(3, TableLayout.Fill.HORIZONTAL);
        gridLayout.setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        gridLayout.setColumnWeightX(2, 1.0);
        gridLayout.setColumnWeightX(3, 1.0);
        gridLayout.setColumnWeightX(4, 1.0);
        grid.setHeaderRow(
                /*0*/ //selection column
                /*1*/ new JLabel("<html><b>Aggregator</b>"),
                /*2*/ new JLabel("<html><b>Source Bands</b>"),
                /*3*/ new JLabel("<html><b>Parameters</b>"),
                /*4*/ new JLabel("<html><b>Target Bands</b>"),
                /*5*/ null // column for edit button
        );
        aggregatorTableController = new AggregatorTableController(grid, binningFormModel);
        final ListControlBar gridControlBar = ListControlBar.create(ListControlBar.HORIZONTAL, grid, aggregatorTableController);

        final AbstractButton sel = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ShowSelection16.png"), true);
        sel.setToolTipText("Show/hide selection column");
        sel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid.setShowSelectionColumn(sel.isSelected());
                gridControlBar.updateState();
            }
        });
        gridControlBar.add(sel, 0);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(gridControlBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createVariablesPanel() {
        final Grid grid = new Grid(4, false);
        TableLayout gridLayout = grid.getLayout();
        gridLayout.setTablePadding(4, 3);
        gridLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        gridLayout.setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        gridLayout.setColumnWeightX(2, 1.0);
        grid.setHeaderRow(
                /*0*/ //selection column
                /*1*/ new JLabel("<html><b>Name</b>"),
                /*2*/ new JLabel("<html><b>Expression</b>"),
                /*5*/ null // column for edit button
        );

        variableTableController = new VariableTableController(grid, binningFormModel);
        final ListControlBar gridControlBar = ListControlBar.create(ListControlBar.HORIZONTAL, grid, variableTableController);

        final AbstractButton sel = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ShowSelection16.png"), true);
        sel.setToolTipText("Show/hide selection column");
        sel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid.setShowSelectionColumn(sel.isSelected());
                gridControlBar.updateState();
            }
        });
        gridControlBar.add(sel, 0);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Intermediate Source Bands (optional)"));
        panel.add(gridControlBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        return panel;
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
            if (Math.abs(currentGridResolution - resolution) > 1E-6) {
                binningFormModel.getBindingContext().getPropertySet().setValue(BinningFormModel.PROPERTY_KEY_NUM_ROWS, computeNumRows(resolution));
                numRowsTextField.setText(String.valueOf(computeNumRows(resolution)));
                currentGridResolution = resolution;
            }
        }
    }

}
