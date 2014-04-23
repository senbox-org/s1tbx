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
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

class AggregatorConfigEditDialog extends ModalDialog {

    private final AggregatorConfig aggregatorConfig;
    private final String[] sourceVarNames;
    private final PropertyContainer aggregatorProperties;

    public AggregatorConfigEditDialog(Window parent, String[] sourceVarNames, AggregatorConfig config) {
        super(parent, "Edit " + config.getName() + " Aggregator", ID_OK | ID_CANCEL, null);
        this.sourceVarNames = sourceVarNames;
        this.aggregatorConfig = config;
        aggregatorProperties = PropertyContainer.createMapBacked(new HashMap<String, Object>(), aggregatorConfig.getClass());
    }

    TargetVariableSpec getSpec() {
        return new TargetVariableSpec();
    }

    @Override
    public int show() {
        setContent(createUI());
        this.getJDialog().getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getJDialog().setResizable(false);
        return super.show();
    }

    @Override
    protected void onOK() {
        PropertySet objectPropertySet = PropertyContainer.createObjectBacked(aggregatorConfig);
        Property[] mapProperties = aggregatorProperties.getProperties();
        for (Property mapProperty : mapProperties) {
            objectPropertySet.setValue(mapProperty.getName(), mapProperty.getValue());
        }
        super.onOK();
    }

    private Component createUI() {
        return createPropertyPane();
    }

    private JComponent createPropertyPane() {
        Property[] properties = aggregatorProperties.getProperties();
        for (Property property : properties) {
            String propertyName = property.getName();
            if("type".equals(propertyName)) {
                property.getDescriptor().setAttribute("visible", false);
            }
            if("varName".equals(propertyName) || "onMaxVarName".equals(propertyName) || "setVarNames".equals(propertyName)) {
                property.getDescriptor().setValueSet(new ValueSet(sourceVarNames));
            }
        }
        aggregatorProperties.setDefaultValues();
        return new PropertyPane(aggregatorProperties).createPanel();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JFrame jFrame = new JFrame();
                jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
                List<AggregatorDescriptor> aggregatorDescriptors = registry.getDescriptors(AggregatorDescriptor.class);
                String[] aggregatorNames = new String[aggregatorDescriptors.size()];
                for (int i = 0; i < aggregatorDescriptors.size(); i++) {
                    AggregatorDescriptor aggregatorDescriptor = aggregatorDescriptors.get(i);
                    String name = aggregatorDescriptor.getName();
                    aggregatorNames[i] = name;
                }
                final JComboBox<String> aggregatorComboBox = new JComboBox<>(aggregatorNames);
                aggregatorComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
                        String aggregatorName = (String) aggregatorComboBox.getSelectedItem();
                        AggregatorDescriptor aggregatorDescriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorName);
                        AggregatorConfig config = aggregatorDescriptor.createConfig();
                        AggregatorConfigEditDialog dialog = new AggregatorConfigEditDialog(jFrame, new String[]{
                                "schere",
                                "stein",
                                "papier",
                                "echse",
                                "spock"
                        }, config);
                        dialog.getJDialog().setLocation(550, 300);
                        dialog.show();

                    }
                });
                jFrame.setContentPane(aggregatorComboBox);
                jFrame.setBounds(300, 300, 200, 80);
                jFrame.setVisible(true);
            }
        });
    }
}
