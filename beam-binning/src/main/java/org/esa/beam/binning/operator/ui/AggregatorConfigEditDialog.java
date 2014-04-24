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
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class AggregatorConfigEditDialog extends ModalDialog {

    private final AggregatorTableController.AggregatorItem aggregatorItem;
    private final String[] sourceVarNames;
    private AggregatorConfig aggregatorConfig;
    private JComboBox<String> aggregatorComboBox;
    private AggregatorDescriptor aggregatorDescriptor;
    private PropertySet aggregatorPropertySet;

    public AggregatorConfigEditDialog(Window parent, String[] sourceVarNames, AggregatorTableController.AggregatorItem aggregatorItem) {
        super(parent, "Edit " + aggregatorItem.aggregatorConfig.getName() + " Aggregator", ID_OK | ID_CANCEL, null);
        this.sourceVarNames = sourceVarNames;
        this.aggregatorItem = aggregatorItem;
        aggregatorConfig = aggregatorItem.aggregatorConfig;
        aggregatorDescriptor = aggregatorItem.aggregatorDescriptor;
        aggregatorPropertySet = createPropertySet(aggregatorConfig);
        PropertySet objectPropertySet = PropertyContainer.createObjectBacked(aggregatorConfig);
        Property[] objectProperties = objectPropertySet.getProperties();
        for (Property objectProperty : objectProperties) {
            aggregatorPropertySet.setValue(objectProperty.getName(), objectProperty.getValue());
        }

    }

    /**
     * @deprecated should not be used anymore
     */
    @Deprecated
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
        AggregatorConfig config = aggregatorDescriptor.createConfig();
        PropertySet objectPropertySet = config.asPropertySet();
        Property[] mapProperties = aggregatorPropertySet.getProperties();
        for (Property mapProperty : mapProperties) {
            objectPropertySet.setValue(mapProperty.getName(), mapProperty.getValue());
        }
        objectPropertySet.setValue("type", aggregatorDescriptor.getName());
        aggregatorItem.aggregatorConfig = config;
        aggregatorItem.aggregatorDescriptor = aggregatorDescriptor;
        super.onOK();
    }

    private Component createUI() {
        return createPropertyPane();
    }

    private JComponent createPropertyPane() {
        final JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        final TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        List<AggregatorDescriptor> aggregatorDescriptors = registry.getDescriptors(AggregatorDescriptor.class);
        List<String> aggregatorNames = new ArrayList<>();
        for (AggregatorDescriptor aggregatorDescriptor : aggregatorDescriptors) {
            aggregatorNames.add(aggregatorDescriptor.getName());
        }
        Collections.sort(aggregatorNames);


        aggregatorComboBox = new JComboBox<>(aggregatorNames.toArray(new String[aggregatorNames.size()]));
        aggregatorComboBox.setSelectedItem(aggregatorConfig.getName());
        aggregatorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aggregatorDescriptor = getDescriptorFromComboBox();
                aggregatorConfig = aggregatorDescriptor.createConfig();
                aggregatorPropertySet = createPropertySet(AggregatorConfigEditDialog.this.aggregatorConfig);
                JPanel aggrPropertyPanel = createPropertyPanel(aggregatorPropertySet);
                mainPanel.remove(1);
                mainPanel.add(aggrPropertyPanel, BorderLayout.CENTER);
                getJDialog().getContentPane().revalidate();
                getJDialog().pack();
            }
        });

        JPanel aggrPropertyPanel = createPropertyPanel(aggregatorPropertySet);


        mainPanel.add(aggregatorComboBox, BorderLayout.NORTH);
        mainPanel.add(aggrPropertyPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    PropertySet createPropertySet(AggregatorConfig config) {
        PropertySet aggregatorPropertySet = PropertyContainer.createMapBacked(new HashMap<String, Object>(), config.getClass(),
                                                                              new ParameterDescriptorFactory());
        aggregatorPropertySet.setDefaultValues();
        return aggregatorPropertySet;
    }

    AggregatorDescriptor getDescriptorFromComboBox() {
        final TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        String aggrType = (String) aggregatorComboBox.getSelectedItem();
        return registry.getDescriptor(AggregatorDescriptor.class, aggrType);
    }

    private JPanel createPropertyPanel(PropertySet propertySet) {
        Property[] properties = propertySet.getProperties();
        for (Property property : properties) {
            String propertyName = property.getName();
            if ("type".equals(propertyName)) {
                property.getDescriptor().setAttribute("visible", false);
            }
            if (AggregatorTableController.isSourcePropertyName(propertyName)) {
                property.getDescriptor().setValueSet(new ValueSet(sourceVarNames));
            }
        }
        propertySet.setDefaultValues();
        return new PropertyPane(propertySet).createPanel();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JFrame jFrame = new JFrame();
                jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                final JButton aggregatorComboBox = new JButton("Show Dialog...");
                aggregatorComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AggregatorConfigEditDialog dialog = new AggregatorConfigEditDialog(jFrame, new String[]{
                                "stein",
                                "papier",
                                "schere",
                                "echse",
                                "spock"
                        }, new AggregatorTableController.AggregatorItem());
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
