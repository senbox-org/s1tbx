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
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.PropertyPane;
import org.apache.commons.lang.ArrayUtils;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author thomas
 */
public class EditTargetVariableDialog extends ModalDialog {

    private static final String PROPERTY_SOURCE_TYPE = "sourceType";
    private final TargetVariableSpec originalTargetVariableSpec;
    private final BinningFormModel binningFormModel;
    private final AppContext appContext;

    private JComboBox<AggregatorDescriptor> aggregatorsComboBox;
    private JPanel aggregatorPanel;
    private JTextField targetPrefix;
    private JTextField dataSource;
    private PropertyContainer aggregatorProperties;

    private Property sourceType;

    public EditTargetVariableDialog(Window parent, TargetVariableSpec originalTargetVariableSpec, BinningFormModel binningFormModel, AppContext appContext) {
        super(parent, "Edit target variable", ID_OK | ID_CANCEL, null);
        this.originalTargetVariableSpec = originalTargetVariableSpec;
        this.binningFormModel = binningFormModel;
        this.appContext = appContext;
        this.sourceType = Property.create(PROPERTY_SOURCE_TYPE, Integer.class);
        sourceType.setContainer(new PropertyContainer());
        setSourceType(TargetVariableSpec.Source.RASTER_SOURCE_TYPE);
        setContent(createUI());
        getJDialog().setResizable(false);
    }

    @Override
    protected void onOK() {
        if (isExpressionSourceType() && StringUtils.isNullOrEmpty(targetPrefix.getText())) {
            JOptionPane.showMessageDialog(getParent(), "Target prefix must be provided if expression is used as source.");
        } else {
            super.onOK();
        }
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
        if (sourceType.getValue() == TargetVariableSpec.Source.RASTER_SOURCE_TYPE) {
            source.bandName = dataSource.getText();
        } else {
            source.expression = dataSource.getText();
        }
        source.type = sourceType.getValue();
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
        final boolean hasSourceProducts = binningFormModel.getSourceProducts().length > 0;

        JPanel panel = new JPanel(layout);

        panel.add(new JLabel("Data source:"));
        dataSource = new JTextField();
        dataSource.setMinimumSize(new Dimension(120, 20));
        dataSource.setPreferredSize(new Dimension(120, 20));
        dataSource.setEnabled(hasSourceProducts);

        panel.add(dataSource);
        final JButton sourceButton = new JButton("...", UIUtils.loadImageIcon("icons/PanelDown12.png"));
        sourceButton.setPreferredSize(new Dimension(40, 20));
        sourceButton.setHorizontalTextPosition(JButton.LEFT);
        sourceButton.addActionListener(new SourceButtonAction(sourceButton));
        sourceButton.setEnabled(hasSourceProducts);
        panel.add(sourceButton);

        panel.add(new JLabel("Target prefix:"));
        targetPrefix = new JTextField();
        panel.add(targetPrefix);
        panel.add(aggregatorPanel);

        applyOriginalVariableSpec();
        targetPrefix.setEnabled(isExpressionSourceType());
        sourceType.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(PROPERTY_SOURCE_TYPE)) {
                    targetPrefix.setEnabled(isExpressionSourceType());
                }
            }
        });

        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
                    boolean enabled = binningFormModel.getSourceProducts().length > 0;
                    dataSource.setEnabled(enabled);
                    sourceButton.setEnabled(enabled);
                }
            }
        });
        return panel;
    }

    private boolean isExpressionSourceType() {
        return sourceType.getValue() == TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE;
    }

    private void applyOriginalVariableSpec() {
        if (originalTargetVariableSpec == null) {
            return;
        }
        targetPrefix.setText(originalTargetVariableSpec.targetPrefix);
        setSourceType(originalTargetVariableSpec.source.type);
        if (sourceType.getValue() == TargetVariableSpec.Source.RASTER_SOURCE_TYPE) {
            dataSource.setText(originalTargetVariableSpec.source.bandName);
        } else {
            dataSource.setText(originalTargetVariableSpec.source.expression);
        }
        aggregatorsComboBox.setSelectedItem(originalTargetVariableSpec.aggregatorDescriptor);
        aggregatorProperties = originalTargetVariableSpec.aggregatorProperties;
        setAggregatorConfigPanel(new PropertyPane(originalTargetVariableSpec.aggregatorProperties).createPanel());
    }

    private void setSourceType(int type) {
        try {
            sourceType.setValue(type);
        } catch (ValidationException e) {
            throw new IllegalStateException("Should never come here", e);
        }
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
        AggregatorConfig aggregatorConfig = selectedAggregatorDescriptor.createConfig();
//        aggregatorProperties = PropertyContainer.createObjectBacked(aggregatorConfig);
        aggregatorProperties = PropertyContainer.createForFields(aggregatorConfig.getClass(),
                                                                 new ParameterDescriptorFactory(),
                                                                 new MyPropertyAccessorFactory(aggregatorConfig), true);
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

    private static class MyPropertyAccessorFactory implements PropertyAccessorFactory {

        private final Object object;

        private MyPropertyAccessorFactory(Object object) {
            this.object = object;
        }

        @Override
        public PropertyAccessor createValueAccessor(Field field) {
            return new ClassFieldAccessor(object, field);
        }
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
            viewPopup.add(new EditBandAction());
            viewPopup.add(new EditExpressionAction());
            viewPopup.show(sourceButton, 1, sourceButton.getBounds().height + 1);
        }

        private class EditBandAction extends AbstractAction {

            public EditBandAction() {
                super("Choose raster data...");
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
                Object chosenRaster = JOptionPane.showInputDialog(getParent(), "Choose raster data",
                                                                  "Choose raster data", JOptionPane.PLAIN_MESSAGE, null,
                                                                  rasterNames, rasterNames[0]);
                if (chosenRaster != null) {
                    dataSource.setText(chosenRaster.toString());
                    setSourceType(TargetVariableSpec.Source.RASTER_SOURCE_TYPE);
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
                String expression = editExpression(dataSource.getText());
                if (expression != null) {
                    dataSource.setText(expression);
                    setSourceType(TargetVariableSpec.Source.EXPRESSION_SOURCE_TYPE);
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
    }
}
