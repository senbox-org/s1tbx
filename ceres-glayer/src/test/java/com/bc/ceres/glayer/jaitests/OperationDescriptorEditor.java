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

package com.bc.ceres.glayer.jaitests;

import javax.media.jai.*;
import javax.media.jai.util.Range;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Provides a Swing component used to edit the parameters of a JAI operation.
 */
public class OperationDescriptorEditor {
    private final OperationDescriptor operationDescriptor;
    private final ArrayList<ParameterDescriptor> parameterDescriptors;
    private final String modeName;

    public OperationDescriptorEditor(OperationDescriptor operationDescriptor) {
        this(operationDescriptor, "rendered");
    }

    public OperationDescriptorEditor(OperationDescriptor operationDescriptor, String modeName) {
        this.operationDescriptor = operationDescriptor;
        this.modeName = modeName;
        final ParameterListDescriptor descriptor = operationDescriptor.getParameterListDescriptor(modeName);
        final String[] names = descriptor.getParamNames();
        final Class[] classes = descriptor.getParamClasses();
        final Object[] defaultValues = descriptor.getParamDefaults();
        parameterDescriptors = new ArrayList<ParameterDescriptor>(names.length);
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            final Class type = classes[i];
            final Object defaultValue = defaultValues[i];

            final Range range = descriptor.getParamValueRange(name);

            final EnumeratedParameter[] enumeratedParameters;
            if (EnumeratedParameter.class.isAssignableFrom(type)) {
                enumeratedParameters = descriptor.getEnumeratedParameterValues(name);
            } else {
                enumeratedParameters = null;
            }
            final ParameterDescriptor parameterDescriptor = new ParameterDescriptor(name, type, defaultValue, range, enumeratedParameters);
            parameterDescriptors.add(parameterDescriptor);
        }
    }

    public OperationDescriptor getOperationDescriptor() {
        return operationDescriptor;
    }

    public String getModeName() {
        return modeName;
    }

    public JPanel createPanel() {
        final GridBagLayout bagLayout = new GridBagLayout();
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(1, 0, 1, 2);
        JPanel panel = new JPanel(bagLayout);

        constraints.gridx = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 0;
        constraints.gridy = 0;
        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            final JLabel label = new JLabel(parameterDescriptor.name + " (" + parameterDescriptor.type.getSimpleName() + "): ");
            final JComponent editor = getEditor(parameterDescriptor);

            constraints.gridx = 0;
            constraints.weightx = 0.5;
            constraints.weighty = 0;
            panel.add(label, constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.5;
            constraints.weighty = 0;
            panel.add(editor, constraints);

            constraints.gridy++;
        }
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        panel.add(new JPanel(), constraints);

        return panel;
    }

    private JComponent getEditor(ParameterDescriptor parameterDescriptor) {
        if (parameterDescriptor.type.equals(Boolean.class)) {
            return new JComboBox(new Object[]{Boolean.TRUE, Boolean.FALSE});
        }
        if (EnumeratedParameter.class.isAssignableFrom(parameterDescriptor.type) &&
                parameterDescriptor.enumeratedParameters != null) {
            final JComboBox comboBox = new JComboBox(parameterDescriptor.enumeratedParameters);
            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setText(((EnumeratedParameter) value).getName());
                    return label;
                }
            });
            return comboBox;
        }
        if (Interpolation.class.isAssignableFrom(parameterDescriptor.type)) {
            final JComboBox comboBox = new JComboBox(INTERPOLATION_VALUES);
            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    final Interpolation interpolation = (Interpolation) value;
                    label.setText(I2S.get(interpolation));
                    return label;
                }
            });
            return comboBox;
        }
        if (Number.class.isAssignableFrom(parameterDescriptor.type)) {
            return new JTextField(6);
        }
        return new JTextField(20);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"FileLoad", "MedianFilter", "Transpose", "Mosaic", "Scale", "Format", "Rescale"};
        }
        final JTabbedPane jTabbedPane = new JTabbedPane();
        for (String opName : args) {
            final OperationDescriptor descriptor = (OperationDescriptor) JAI.getDefaultInstance().getOperationRegistry().getDescriptor("rendered", opName);
            final OperationDescriptorEditor descriptorEditor = new OperationDescriptorEditor(descriptor);
            final JPanel panel = descriptorEditor.createPanel();
            jTabbedPane.add(opName, new JScrollPane(panel));
        }
        final JFrame frame = new JFrame("OperationDescriptorEditor-Test");
        frame.add(jTabbedPane, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    final static HashMap<Interpolation, String> I2S = new HashMap<Interpolation, String>(8);
    final static Interpolation[] INTERPOLATION_VALUES = new Interpolation[]{
            Interpolation.getInstance(Interpolation.INTERP_NEAREST),
            Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
            Interpolation.getInstance(Interpolation.INTERP_BICUBIC),
            Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2),
    };
    final static String[] INTERPOLATION_NAMES = new String[]{
            "INTERP_NEAREST",
            "INTERP_BILINEAR",
            "INTERP_BICUBIC",
            "INTERP_BICUBIC_2",
    };

    static {
        for (int i = 0; i < INTERPOLATION_VALUES.length; i++) {
            Interpolation interpolation = INTERPOLATION_VALUES[i];
            I2S.put(interpolation, INTERPOLATION_NAMES[i]);
        }
    }

    private final class ParameterDescriptor {
        final String name;
        final Class type;
        final Object defaultValue;
        final Range range;
        final EnumeratedParameter[] enumeratedParameters;

        ParameterDescriptor(String name, Class type, Object defaultValue, Range range, EnumeratedParameter[] enumeratedParameters) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.range = range;
            this.enumeratedParameters = enumeratedParameters;
        }
    }
}

