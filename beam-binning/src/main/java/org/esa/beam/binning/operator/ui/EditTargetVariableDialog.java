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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author thomas
 */
public class EditTargetVariableDialog extends ModalDialog {

    private TargetVariableSpec targetVariableSpec;

    private JComboBox<AggregatorDescriptor> aggregatorsComboBox;
    private JPanel aggregatorPanel;
    private PropertyContainer aggregatorProperties;

    public EditTargetVariableDialog(Window parent, TargetVariableSpec targetVariableSpec) {
        super(parent, "Edit target variable", ID_OK | ID_CANCEL, null);
        this.targetVariableSpec = targetVariableSpec;
        setContent(createUI());
        getJDialog().setResizable(false);
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
        panel.add(aggregatorPanel);

        applyOriginalVariableSpec();

        return panel;
    }

    private void applyOriginalVariableSpec() {
        if (targetVariableSpec == null) {
            return;
        }
        if (targetVariableSpec.aggregatorDescriptor == null) {
            targetVariableSpec.aggregatorDescriptor = aggregatorsComboBox.getItemAt(0);
            targetVariableSpec.aggregatorProperties = VariableConfigTable.createAggregatorProperties(targetVariableSpec.aggregatorDescriptor);
            VariableConfigTable.removeProperties(targetVariableSpec.aggregatorProperties, "varName", "type", "targetName");
        }
        aggregatorsComboBox.setSelectedItem(targetVariableSpec.aggregatorDescriptor);
        aggregatorProperties = targetVariableSpec.aggregatorProperties;
        setAggregatorConfigPanel(new PropertyPane(targetVariableSpec.aggregatorProperties).createPanel());
    }

    private void setAggregatorConfigPanel(JPanel aggregatorConfigPanel) {
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
                JPanel aggregatorConfigPanel = getPropertyPane(aggregatorsComboBox);
                setAggregatorConfigPanel(aggregatorConfigPanel);
                getJDialog().getContentPane().revalidate();
                getJDialog().pack();
            }
        });
    }

    private JPanel getPropertyPane(JComboBox<AggregatorDescriptor> aggregatorsComboBox) {
        AggregatorDescriptor selectedAggregatorDescriptor = (AggregatorDescriptor) aggregatorsComboBox.getSelectedItem();
        aggregatorProperties = VariableConfigTable.createAggregatorProperties(selectedAggregatorDescriptor);
        VariableConfigTable.removeProperties(aggregatorProperties, "varName", "type", "targetName");
        return new PropertyPane(aggregatorProperties).createPanel();
    }

}
