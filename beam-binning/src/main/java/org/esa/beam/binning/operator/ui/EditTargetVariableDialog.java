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
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author thomas
 */
public class EditTargetVariableDialog extends ModalDialog {

    private final TargetVariableSpec originalTargetVariableSpec;
    private final BinningFormModel binningFormModel;
    private final AppContext appContext;

    private JComboBox<AggregatorDescriptor> aggregatorsComboBox;
    private JPanel aggregatorPanel;
    private JTextField targetPrefix;
    private JTextField dataSource;
    private PropertyContainer aggregatorProperties;

    private int sourceType;

    public EditTargetVariableDialog(Window parent, TargetVariableSpec originalTargetVariableSpec, BinningFormModel binningFormModel, AppContext appContext) {
        super(parent, "Edit target variable", ID_OK | ID_CANCEL, null);
        this.originalTargetVariableSpec = originalTargetVariableSpec;
        this.binningFormModel = binningFormModel;
        this.appContext = appContext;
        setContent(createUI());
        getJDialog().setResizable(false);
    }

    TargetVariableSpec getSpec() {
        TargetVariableSpec targetVariableSpec = new TargetVariableSpec();
        targetVariableSpec.targetPrefix = targetPrefix.getText();
        targetVariableSpec.aggregatorProperties = aggregatorProperties;
        targetVariableSpec.aggregationString = createAggregationString();
        targetVariableSpec.source = createSource();
        targetVariableSpec.aggregatorDescriptor = (AggregatorDescriptor) aggregatorsComboBox.getSelectedItem();
        return targetVariableSpec;
    }

    private TargetVariableSpec.Source createSource() {
        TargetVariableSpec.Source source = new TargetVariableSpec.Source();
        if (sourceType == TargetVariableSpec.Source.BAND_SOURCE_TYPE) {
            source.bandName = dataSource.getText();
        } else {
            source.expression = dataSource.getText();
        }
        source.type = sourceType;
        return source;
    }

    private String createAggregationString() {
        AggregatorDescriptor selectedItem = (AggregatorDescriptor) aggregatorsComboBox.getSelectedItem();
        StringBuilder aggregationString = new StringBuilder(selectedItem.getName());
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
            value = property.getValue().toString();
        }
        return value;
    }

    private Component createUI() {
        TableLayout layout = new TableLayout(3);
        layout.setCellColspan(1, 1, 2);
        layout.setCellColspan(2, 0, 3);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setCellAnchor(2, 0, TableLayout.Anchor.SOUTHEAST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTablePadding(10, 10);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setRowFill(0, TableLayout.Fill.HORIZONTAL);
        layout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        layout.setRowFill(2, TableLayout.Fill.BOTH);

        aggregatorPanel = new JPanel(new BorderLayout());
        aggregatorsComboBox = new JComboBox<>();
        initComboBox();
        aggregatorPanel.add(aggregatorsComboBox, BorderLayout.NORTH);

        setAggregatorConfigPanel(getPropertyPane(aggregatorsComboBox));

        JPanel panel = new JPanel(layout);

        panel.add(new JLabel("Data source:"));
        dataSource = new JTextField();
        dataSource.setMinimumSize(new Dimension(120, 20));
        dataSource.setPreferredSize(new Dimension(120, 20));
        panel.add(dataSource);
        final JButton sourceButton = new JButton("...");
        sourceButton.addActionListener(new SourceButtonAction(sourceButton));
        panel.add(sourceButton);

        panel.add(new JLabel("Target prefix:"));
        targetPrefix = new JTextField();
        panel.add(targetPrefix);
        panel.add(aggregatorPanel);

        applyOriginalVariableSpec();

        return panel;
    }

    private void applyOriginalVariableSpec() {
        if (originalTargetVariableSpec == null) {
            return;
        }
        targetPrefix.setText(originalTargetVariableSpec.targetPrefix);
        sourceType = originalTargetVariableSpec.source.type;
        if (sourceType == TargetVariableSpec.Source.BAND_SOURCE_TYPE) {
            dataSource.setText(originalTargetVariableSpec.source.bandName);
        } else {
            dataSource.setText(originalTargetVariableSpec.source.expression);
        }
        aggregatorsComboBox.setSelectedItem(originalTargetVariableSpec.aggregatorDescriptor);
        aggregatorProperties = originalTargetVariableSpec.aggregatorProperties;
        setAggregatorConfigPanel(new PropertyPane(originalTargetVariableSpec.aggregatorProperties).createPanel());
    }

    private void setAggregatorConfigPanel(JPanel aggregatorConfigPanel) {
        if (aggregatorPanel.getComponents().length > 1) {
            aggregatorPanel.remove(1);
        }
        aggregatorPanel.add(aggregatorConfigPanel, BorderLayout.CENTER);
    }

    private void initComboBox() {
        final List<AggregatorDescriptor> aggregatorDescriptors = getAggregatorDescriptors(AggregatorAverageML.Descriptor.NAME);
        AggregatorDescriptor[] initialiser = aggregatorDescriptors.toArray(new AggregatorDescriptor[aggregatorDescriptors.size()]);
        aggregatorsComboBox.setModel(new DefaultComboBoxModel<>(initialiser));
        aggregatorsComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                AggregatorDescriptor desc = (AggregatorDescriptor) value;
                return super.getListCellRendererComponent(list, desc.getName(), index, isSelected, cellHasFocus);
            }
        });
        aggregatorsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel aggregatorConfigPanel = getPropertyPane(aggregatorsComboBox);
                setAggregatorConfigPanel(aggregatorConfigPanel);
                getJDialog().getContentPane().revalidate();
                getJDialog().pack();
            }
        });
    }

    private JPanel getPropertyPane(JComboBox<AggregatorDescriptor> aggregatorsComboBox) {
        AggregatorDescriptor selectedAggregatorDescriptor = (AggregatorDescriptor) aggregatorsComboBox.getSelectedItem();
        aggregatorProperties = PropertyContainer.createObjectBacked(selectedAggregatorDescriptor.createConfig());
        removeProperties(aggregatorProperties, "varName", "type");
        return new PropertyPane(aggregatorProperties).createPanel();
    }

    private void removeProperties(PropertyContainer propertyContainer, String... properties) {
        for (String property : properties) {
            Property varNameProperty = propertyContainer.getProperty(property);
            if (varNameProperty != null) {
                propertyContainer.removeProperty(varNameProperty);
            }
        }
    }

    private List<AggregatorDescriptor> getAggregatorDescriptors(String... filterNames) {
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

    private class SourceButtonAction implements ActionListener {

        private final JButton sourceButton;

        public SourceButtonAction(JButton sourceButton) {
            this.sourceButton = sourceButton;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPopupMenu viewPopup = new JPopupMenu();
            viewPopup.setBorderPainted(false);
            viewPopup.add(new AbstractAction("Band") {

                @Override
                public boolean isEnabled() {
                    return binningFormModel.getSourceProducts().length > 0;
                }

                @Override
                public void actionPerformed(ActionEvent ignored) {
                    System.out.println("1");
                }
            });
            viewPopup.add(new AbstractAction("Expression") {

                @Override
                public boolean isEnabled() {
                    return binningFormModel.getSourceProducts().length > 0;
                }

                @Override
                public void actionPerformed(ActionEvent ignored) {
                    String expression = editExpression(dataSource.getText());
                    if (expression != null) {
                        dataSource.setText(expression);
                        sourceType = TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE;
                    }
                }
            });
            viewPopup.show(sourceButton, 1, sourceButton.getBounds().height + 1);
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
}
