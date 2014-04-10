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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author thomas
 */
public class EditAggregationDialog extends ModalDialog {

    private TargetVariableSpec targetVariableSpec;

    private JComboBox<AggregatorDescriptor> aggregatorsComboBox;
    private JPanel aggregatorPanel;
    private PropertyContainer aggregatorProperties;

    public EditAggregationDialog(Window parent, TargetVariableSpec targetVariableSpec) {
        super(parent, getTitle(targetVariableSpec), ID_OK | ID_CANCEL, null);
        this.targetVariableSpec = targetVariableSpec;
        this.getJDialog().getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContent(createUI());
        getJDialog().setResizable(false);
    }

    private static String getTitle(TargetVariableSpec targetVariableSpec) {
        return "Edit aggregation" + (StringUtils.isNotNullAndNotEmpty(targetVariableSpec.targetName) ?
                                     " of " + targetVariableSpec.targetName : "");
    }

    TargetVariableSpec getSpec() {
        if (targetVariableSpec == null) {
            targetVariableSpec = new TargetVariableSpec();
            targetVariableSpec.source = new TargetVariableSpec.Source();
        }
        targetVariableSpec.aggregatorProperties = aggregatorProperties;
        AggregatorDescriptor aggregatorDescriptor = (AggregatorDescriptor) aggregatorsComboBox.getSelectedItem();
        targetVariableSpec.aggregationString = VariableConfigTable.createAggregationString(aggregatorDescriptor, aggregatorProperties);
        targetVariableSpec.aggregatorDescriptor = aggregatorDescriptor;
        return targetVariableSpec;
    }

    private Component createUI() {
        aggregatorPanel = new JPanel(new BorderLayout(10, 5));
        aggregatorsComboBox = new JComboBox<>();
        initComboBox();
        aggregatorPanel.add(aggregatorsComboBox, BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(aggregatorPanel, BorderLayout.CENTER);

        applyOriginalVariableSpec();

        return panel;
    }

    private void applyOriginalVariableSpec() {
        if (targetVariableSpec.aggregatorDescriptor == null) {
            targetVariableSpec.aggregatorDescriptor = aggregatorsComboBox.getItemAt(0);
            targetVariableSpec.aggregatorProperties = VariableConfigTable.createAggregatorProperties(targetVariableSpec.aggregatorDescriptor);
            VariableConfigTable.removeProperties(targetVariableSpec.aggregatorProperties, "varName", "type", "targetName");
        }
        aggregatorsComboBox.setSelectedItem(targetVariableSpec.aggregatorDescriptor);
        aggregatorProperties = targetVariableSpec.aggregatorProperties;
        setAggregatorConfigPanel(getPropertyPane(targetVariableSpec.aggregatorDescriptor));
    }

    private void setAggregatorConfigPanel(JComponent aggregatorConfigPanel) {
        if (aggregatorPanel.getComponents().length > 1) {
            aggregatorPanel.remove(1);
        }
        aggregatorPanel.add(aggregatorConfigPanel, BorderLayout.CENTER);
    }

    private void initComboBox() {
        final List<AggregatorDescriptor> aggregatorDescriptors = VariableConfigTable.getAggregatorDescriptors(AggregatorAverageML.Descriptor.NAME);
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
                JComponent aggregatorConfigPanel = getPropertyPane((AggregatorDescriptor) aggregatorsComboBox.getSelectedItem());
                setAggregatorConfigPanel(aggregatorConfigPanel);
                getJDialog().getContentPane().revalidate();
                getJDialog().pack();
            }
        });
    }

    private JComponent getPropertyPane(AggregatorDescriptor selectedAggregatorDescriptor) {
        if (aggregatorProperties == null) {
            aggregatorProperties = VariableConfigTable.createAggregatorProperties(selectedAggregatorDescriptor);
            VariableConfigTable.removeProperties(aggregatorProperties, "varName", "type", "targetName");
        }
        PropertyContainer newAggProps = VariableConfigTable.createAggregatorProperties(selectedAggregatorDescriptor);
        VariableConfigTable.removeProperties(newAggProps, "varName", "type", "targetName");
        if (!haveSameDescriptor(newAggProps, aggregatorProperties)) {
            this.aggregatorProperties = newAggProps;
        }

        AggregatorConfig config = selectedAggregatorDescriptor.createConfig();
        PropertyEditor propertyEditor = config.getExtension(PropertyEditor.class);
        if (propertyEditor != null) {
            BindingContext bindingContext = new BindingContext();
            Property property = Property.create("aggregatorProperties", aggregatorProperties);
            bindingContext.getPropertySet().addProperty(property);
            return propertyEditor.createEditorComponent(property.getDescriptor(), bindingContext);
        } else {
            return new PropertyPane(aggregatorProperties).createPanel();
        }
    }

    private static boolean haveSameDescriptor(PropertyContainer newAggProps, PropertyContainer aggregatorProperties) {
        for (Property newProp : newAggProps.getProperties()) {
            if (aggregatorProperties.getProperty(newProp.getName()) == null) {
                return false;
            }
        }
        for (Property oldProp : aggregatorProperties.getProperties()) {
            if (newAggProps.getProperty(oldProp.getName()) == null) {
                return false;
            }
        }
        return true;
    }

}
