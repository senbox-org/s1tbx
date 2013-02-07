/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pixex.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.combobox.DefaultDateModel;
import com.jidesoft.grid.DateCellEditor;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.FloatCellEditor;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.pixex.Coordinate;
import org.esa.beam.pixex.PixExOp;
import org.jfree.ui.DateCellRenderer;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

class PixelExtractionParametersForm {

    private static final ImageIcon ADD_ICON = UIUtils.loadImageIcon("icons/Plus24.gif");
    private static final ImageIcon REMOVE_ICON = UIUtils.loadImageIcon("icons/Minus24.gif");

    private JPanel mainPanel;
    private JLabel windowLabel;
    private JSpinner windowSpinner;
    private final AppContext appContext;

    private final CoordinateTableModel coordinateTableModel;
    private JButton editExpressionButton;
    private JCheckBox useExpressionCheckBox;
    private JTextArea expressionArea;
    private JRadioButton expressionAsFilterButton;
    private JRadioButton exportExpressionResultButton;
    private Product activeProduct;
    private JLabel expressionNoteLabel;
    private JSpinner timeSpinner;
    private JComboBox timeUnitComboBox;
    private String allowedTimeDifference = "";
    private JComboBox aggregationStrategyChooser;
    private JCheckBox includeOriginalInputBox;

    PixelExtractionParametersForm(AppContext appContext, PropertyContainer container) {
        this.appContext = appContext;
        coordinateTableModel = new CoordinateTableModel();
        createUi(container);
        updateUi();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public Coordinate[] getCoordinates() {
        Coordinate[] coordinates = new Coordinate[coordinateTableModel.getRowCount()];
        for (int i = 0; i < coordinateTableModel.getRowCount(); i++) {
            final Placemark placemark = coordinateTableModel.getPlacemarkAt(i);
            SimpleFeature feature = placemark.getFeature();
            final Date dateTime = (Date) feature.getAttribute(Placemark.PROPERTY_NAME_DATETIME);
            final Coordinate.OriginalValue[] originalValues = PixExOp.getOriginalValues(feature);
            if (placemark.getGeoPos() == null) {
                final Point point = (Point) feature.getDefaultGeometry();
                coordinates[i] = new Coordinate(placemark.getName(), (float) point.getY(), (float) point.getX(),
                                                dateTime, originalValues);
            } else {
                coordinates[i] = new Coordinate(placemark.getName(), placemark.getGeoPos().getLat(),
                                                placemark.getGeoPos().getLon(), dateTime, originalValues);
            }
        }
        return coordinates;
    }

    public String getExpression() {
        if (useExpressionCheckBox.isSelected()) {
            return expressionArea.getText();
        } else {
            return null;
        }
    }

    public String getAllowedTimeDifference() {
        return allowedTimeDifference;
    }

    public boolean isExportExpressionResultSelected() {
        return exportExpressionResultButton.isSelected();
    }

    public String getPixelValueAggregationMethod() {
        return aggregationStrategyChooser.getSelectedItem().toString();
    }

    private void createUi(PropertyContainer container) {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellFill(0, 1, TableLayout.Fill.BOTH); // coordinate table
        tableLayout.setCellWeightY(0, 1, 7.0);
        tableLayout.setCellPadding(6, 0, new Insets(8, 4, 4, 4)); // expression label
        tableLayout.setCellPadding(6, 1, new Insets(0, 0, 0, 0)); // expression panel
        tableLayout.setCellWeightX(6, 1, 1.0);
        tableLayout.setCellWeightY(6, 1, 3.0);
        tableLayout.setCellFill(6, 1, TableLayout.Fill.BOTH);
        tableLayout.setCellPadding(7, 0, new Insets(8, 4, 4, 4)); // Sub-scene label
        tableLayout.setCellPadding(7, 1, new Insets(0, 0, 0, 0));
        tableLayout.setCellPadding(8, 0, new Insets(8, 4, 4, 4)); // kmz export label
        tableLayout.setCellPadding(8, 1, new Insets(0, 0, 0, 0));
        tableLayout.setCellPadding(9, 0, new Insets(8, 4, 4, 4)); // output match label
        tableLayout.setCellPadding(9, 1, new Insets(0, 0, 0, 0));

        mainPanel = new JPanel(tableLayout);
        mainPanel.add(new JLabel("Coordinates:"));
        final JComponent[] coordinatesComponents = createCoordinatesComponents();
        mainPanel.add(coordinatesComponents[0]);
        mainPanel.add(coordinatesComponents[1]);
        final Component[] timeDeltaComponents = createTimeDeltaComponents(tableLayout);
        for (Component timeDeltaComponent : timeDeltaComponents) {
            mainPanel.add(timeDeltaComponent);
        }

        coordinateTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateIncludeOriginalInputBox();
            }
        });

        final BindingContext bindingContext = new BindingContext(container);

        mainPanel.add(new JLabel("Export:"));
        mainPanel.add(createExportPanel(bindingContext));
        mainPanel.add(tableLayout.createHorizontalSpacer());

        mainPanel.add(new JLabel("Window size:"));
        windowSpinner = createWindowSizeEditor(bindingContext);
        windowLabel = new JLabel();
        windowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        windowSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                handleWindowSpinnerChange();
            }
        });
        mainPanel.add(windowSpinner);
        mainPanel.add(windowLabel);

        mainPanel.add(new JLabel("Pixel value aggregation method:"));
        aggregationStrategyChooser = new JComboBox(new String[]{
                PixExOp.NO_AGGREGATION,
                PixExOp.MEAN_AGGREGATION,
                PixExOp.MIN_AGGREGATION,
                PixExOp.MAX_AGGREGATION,
                PixExOp.MEDIAN_AGGREGATION
        });
        mainPanel.add(aggregationStrategyChooser);
        mainPanel.add(tableLayout.createVerticalSpacer());

        mainPanel.add(new JLabel("Expression:"));
        mainPanel.add(createExpressionPanel(bindingContext));
        mainPanel.add(tableLayout.createHorizontalSpacer());

        mainPanel.add(new JLabel("Sub-scenes:"));
        mainPanel.add(createSubSceneExportPanel(bindingContext));
        mainPanel.add(tableLayout.createHorizontalSpacer());

        mainPanel.add(new JLabel("KMZ coordinates:"));
        mainPanel.add(createKmzExportPanel(bindingContext));
        mainPanel.add(tableLayout.createHorizontalSpacer());

        mainPanel.add(new JLabel("Match with original input:"));
        mainPanel.add(createIncludeOriginalInputBox(bindingContext));
        mainPanel.add(tableLayout.createHorizontalSpacer());

    }

    private JComponent createIncludeOriginalInputBox(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        final JPanel panel = new JPanel(tableLayout);
        includeOriginalInputBox = new JCheckBox("Include original input");
        bindingContext.bind("includeOriginalInput", includeOriginalInputBox);
        panel.add(includeOriginalInputBox);
        updateIncludeOriginalInputBox();
        return panel;
    }

    private Component createKmzExportPanel(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        final JPanel panel = new JPanel(tableLayout);
        final JCheckBox exportKmzBox = new JCheckBox("Export found coordinates in Google KMZ format");
        bindingContext.bind("exportKmz", exportKmzBox);
        panel.add(exportKmzBox);
        return panel;
    }

    private JPanel createSubSceneExportPanel(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(4);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(1, 0.3);
        tableLayout.setColumnWeightX(3, 1.0);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        final JPanel exportPanel = new JPanel(tableLayout);
        final JCheckBox exportSubScenesCheckBox = new JCheckBox("Enable export");
        final JLabel borderSizeLabel = new JLabel("Border size:");
        final JTextField borderSizeTextField = new JTextField();
        borderSizeTextField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind("exportSubScenes", exportSubScenesCheckBox);
        bindingContext.bind("subSceneBorderSize", borderSizeTextField);
        bindingContext.bindEnabledState("subSceneBorderSize", false, "exportSubScenes", false);
        exportPanel.add(exportSubScenesCheckBox);
        exportPanel.add(new JLabel());
        exportPanel.add(borderSizeLabel);
        exportPanel.add(borderSizeTextField);
        return exportPanel;
    }

    private Component[] createTimeDeltaComponents(TableLayout tableLayout) {
        final JLabel boxLabel = new JLabel("Allowed time difference:");
        final JCheckBox box = new JCheckBox("Use time difference constraint");
        final Component horizontalSpacer = tableLayout.createHorizontalSpacer();

        final Component horizontalSpacer2 = tableLayout.createHorizontalSpacer();
        timeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        timeSpinner.setEnabled(false);
        timeSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                allowedTimeDifference = createAllowedTimeDifferenceString();
            }
        });
        timeUnitComboBox = new JComboBox(new String[]{"Day(s)", "Hour(s)", "Minute(s)"});
        timeUnitComboBox.setEnabled(false);
        timeUnitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                allowedTimeDifference = createAllowedTimeDifferenceString();
            }
        });

        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeSpinner.setEnabled(box.isSelected());
                timeUnitComboBox.setEnabled(box.isSelected());
                allowedTimeDifference = box.isSelected() ? createAllowedTimeDifferenceString() : "";
            }
        });

        allowedTimeDifference = box.isSelected() ? createAllowedTimeDifferenceString() : "";
        return new Component[]{boxLabel, box, horizontalSpacer, horizontalSpacer2, timeSpinner, timeUnitComboBox};
    }

    private String createAllowedTimeDifferenceString() {
        return String.valueOf(
                timeSpinner.getValue()) + timeUnitComboBox.getSelectedItem().toString().charAt(0);
    }

    private void updateUi() {
        handleWindowSpinnerChange();
        updateExpressionComponents();
        updateIncludeOriginalInputBox();
    }

    private void updateIncludeOriginalInputBox() {
        includeOriginalInputBox.setEnabled(false);
        final Coordinate[] coordinates = getCoordinates();
        for (Coordinate coordinate : coordinates) {
            if (coordinate.getOriginalValues().length > 0) {
                includeOriginalInputBox.setEnabled(true);
                return;
            }
        }
    }

    private void updateExpressionComponents() {
        final boolean useExpressionSelected = useExpressionCheckBox.isSelected();
        editExpressionButton.setEnabled(useExpressionSelected && activeProduct != null);
        String toolTip = null;
        if (activeProduct == null) {
            toolTip = String.format("Editor can only be used with a product opened in %s.",
                                    appContext.getApplicationName());
        }
        editExpressionButton.setToolTipText(toolTip);
        expressionArea.setEnabled(useExpressionSelected);
        expressionNoteLabel.setEnabled(useExpressionSelected);
        expressionAsFilterButton.setEnabled(useExpressionSelected);
        exportExpressionResultButton.setEnabled(useExpressionSelected);
    }

    private void handleWindowSpinnerChange() {
        final Integer windowSize = (Integer) windowSpinner.getValue();
        windowLabel.setText(String.format("%1$d x %1$d", windowSize));
        final boolean pixelsNeedToBeAggregated = windowSize > 1;
        aggregationStrategyChooser.setEnabled(pixelsNeedToBeAggregated);
    }

    private JPanel createExportPanel(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(4);
        tableLayout.setTablePadding(4, 0);
        tableLayout.setTableFill(TableLayout.Fill.VERTICAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnWeightX(3, 1.0);
        tableLayout.setTableWeightY(1.0);
        final JPanel exportPanel = new JPanel(tableLayout);

        exportPanel.add(createIncludeCheckbox(bindingContext, "Bands", "exportBands"));
        exportPanel.add(createIncludeCheckbox(bindingContext, "Tie-point grids", "exportTiePoints"));
        exportPanel.add(createIncludeCheckbox(bindingContext, "Masks", "exportMasks"));
        exportPanel.add(tableLayout.createHorizontalSpacer());
        return exportPanel;
    }


    private JCheckBox createIncludeCheckbox(BindingContext bindingContext, String labelText, String propertyName) {
        final Property windowProperty = bindingContext.getPropertySet().getProperty(propertyName);
        final Boolean defaultValue = (Boolean) windowProperty.getDescriptor().getDefaultValue();
        final JCheckBox checkbox = new JCheckBox(labelText, defaultValue);
        bindingContext.bind(propertyName, checkbox);
        return checkbox;
    }

    private JPanel createExpressionPanel(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setCellAnchor(0, 1, TableLayout.Anchor.NORTHEAST); // edit expression button
        tableLayout.setRowFill(0, TableLayout.Fill.VERTICAL);
        tableLayout.setCellFill(1, 0, TableLayout.Fill.BOTH); // expression text area
        tableLayout.setCellWeightY(1, 0, 1.0);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellColspan(2, 0, 2); // expression note line 1
        tableLayout.setCellColspan(3, 0, 2); // radio button group
        tableLayout.setCellFill(3, 0, TableLayout.Fill.BOTH);
        final JPanel panel = new JPanel(tableLayout);

        useExpressionCheckBox = new JCheckBox("Use expression");
        useExpressionCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateExpressionComponents();
            }
        });
        editExpressionButton = new JButton("Edit Expression...");
        final Window parentWindow = SwingUtilities.getWindowAncestor(panel);
        editExpressionButton.addActionListener(new EditExpressionActionListener(parentWindow));
        panel.add(useExpressionCheckBox);
        panel.add(editExpressionButton);
        expressionArea = new JTextArea(3, 40);
        expressionArea.setLineWrap(true);
        panel.add(new JScrollPane(expressionArea));

        expressionNoteLabel = new JLabel("Note: The expression might not be applicable to all products.");
        panel.add(expressionNoteLabel);

        final ButtonGroup buttonGroup = new ButtonGroup();
        expressionAsFilterButton = new JRadioButton("Use expression as filter", true);
        buttonGroup.add(expressionAsFilterButton);
        exportExpressionResultButton = new JRadioButton("Export expression result");
        buttonGroup.add(exportExpressionResultButton);
        final Property exportResultProperty = bindingContext.getPropertySet().getProperty("exportExpressionResult");
        final Boolean defaultValue = (Boolean) exportResultProperty.getDescriptor().getDefaultValue();
        exportExpressionResultButton.setSelected(defaultValue);
        exportExpressionResultButton.setToolTipText(
                "Expression result is exported to the output file for each exported pixel.");
        expressionAsFilterButton.setSelected(!defaultValue);
        expressionAsFilterButton.setToolTipText(
                "Expression is used as filter (all pixels in given window must be valid).");

        final JPanel expressionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expressionButtonPanel.add(expressionAsFilterButton);
        expressionButtonPanel.add(exportExpressionResultButton);
        panel.add(expressionButtonPanel);
        return panel;
    }

    private JComponent[] createCoordinatesComponents() {
        Product selectedProduct = appContext.getSelectedProduct();
        if (selectedProduct != null) {
            final PlacemarkGroup pinGroup = selectedProduct.getPinGroup();
            for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                coordinateTableModel.addPlacemark(pinGroup.get(i));
            }
        }

        JTable coordinateTable = new JTable(coordinateTableModel);
        coordinateTable.setName("coordinateTable");
        coordinateTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        coordinateTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        coordinateTable.setRowSelectionAllowed(true);
        coordinateTable.getTableHeader().setReorderingAllowed(false);
        coordinateTable.setDefaultRenderer(Float.class, new DecimalTableCellRenderer(new DecimalFormat("0.0000")));
        coordinateTable.setPreferredScrollableViewportSize(new Dimension(250, 100));
        coordinateTable.getColumnModel().getColumn(1).setCellEditor(new FloatCellEditor(-90, 90));
        coordinateTable.getColumnModel().getColumn(2).setCellEditor(new FloatCellEditor(-180, 180));
        coordinateTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        final DefaultDateModel dateModel = new DefaultDateModel();
        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // ISO 8601
        dateModel.setDateFormat(dateFormat);
        final DateCellEditor dateCellEditor = new DateCellEditor(dateModel, true);
        dateCellEditor.setTimeDisplayed(true);
        coordinateTable.getColumnModel().getColumn(3).setCellEditor(dateCellEditor);
        coordinateTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        final DateCellRenderer dateCellRenderer = new DateCellRenderer(dateFormat);
        dateCellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        coordinateTable.getColumnModel().getColumn(3).setCellRenderer(dateCellRenderer);
        final JScrollPane rasterScrollPane = new JScrollPane(coordinateTable);

        final AbstractButton addButton = ToolButtonFactory.createButton(ADD_ICON, false);
        addButton.addActionListener(new AddPopupListener());
        final AbstractButton removeButton = ToolButtonFactory.createButton(REMOVE_ICON, false);
        removeButton.addActionListener(new RemovePlacemarksListener(coordinateTable, coordinateTableModel));
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        return new JComponent[]{rasterScrollPane, buttonPanel};
    }

    private JSpinner createWindowSizeEditor(BindingContext bindingContext) {
        final Property windowSizeProperty = bindingContext.getPropertySet().getProperty("windowSize");
        final Number defaultValue = (Number) windowSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Object value = spinner.getValue();
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue % 2 == 0) {
                        spinner.setValue(intValue + 1);
                    }
                }
            }
        });
        bindingContext.bind("windowSize", spinner);
        return spinner;
    }

    public void setActiveProduct(Product product) {
        activeProduct = product;
        updateExpressionComponents();
    }

    private class AddPopupListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final JPopupMenu popup = new JPopupMenu("Add");
            final Object source = e.getSource();
            if (source instanceof Component) {
                final Component component = (Component) source;
                final Rectangle buttonBounds = component.getBounds();
                popup.add(new AddCoordinateAction(coordinateTableModel));
                popup.add(new AddPlacemarkFileAction(appContext, coordinateTableModel, mainPanel));
                popup.add(new AddCsvFileAction(appContext, coordinateTableModel, mainPanel));
                popup.show(component, 1, buttonBounds.height + 1);
            }
        }
    }

    private static class RemovePlacemarksListener implements ActionListener {

        private final JTable coordinateTable;
        private final CoordinateTableModel tableModel;

        private RemovePlacemarksListener(JTable coordinateTable, CoordinateTableModel tableModel) {
            this.coordinateTable = coordinateTable;
            this.tableModel = tableModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = coordinateTable.getSelectedRows();
            Placemark[] toRemove = new Placemark[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                toRemove[i] = tableModel.getPlacemarkAt(selectedRows[i]);
            }
            for (Placemark placemark : toRemove) {
                tableModel.removePlacemark(placemark);
            }
        }
    }

    private class EditExpressionActionListener implements ActionListener {

        private final Window parentWindow;

        private EditExpressionActionListener(Window parentWindow) {
            this.parentWindow = parentWindow;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ProductExpressionPane pep = ProductExpressionPane.createBooleanExpressionPane(new Product[]{activeProduct},
                                                                                          activeProduct,
                                                                                          appContext.getPreferences());
            pep.setCode(expressionArea.getText());
            final int i = pep.showModalDialog(parentWindow, "Expression Editor");
            if (i == ModalDialog.ID_OK) {
                expressionArea.setText(pep.getCode());
            }

        }
    }
}