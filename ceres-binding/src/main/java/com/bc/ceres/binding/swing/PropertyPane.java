package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A utility class used to create a {@link JPanel} containg default Swing components and their corresponding bindings for the
 * {@link ValueContainer} given by the {@link BindingContext}.
 * <p/>
 * <p>If the {@code displayName} property of a {@link ValueDescriptor ValueDescriptor} is set, it will be used as label, otherwise
 * a label is derived from the {@code name} property.</p>
 */
public class PropertyPane {
    private final BindingContext bindingContext;

    public PropertyPane(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {
        ValueContainer valueContainer = bindingContext.getValueContainer();
        ValueModel[] models = valueContainer.getModels();
        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.BASELINE;
        gbc.insets.top = 1;
        gbc.insets.bottom = 1;
        gbc.insets.right = 1;
        gbc.insets.left = 1;
        gbc.gridy = 0;
        for (ValueModel model : models) {
            JComponent editorComponent;
            if (Number.class.isAssignableFrom(model.getDescriptor().getType())) {
                final JTextField textField = new JTextField();
                textField.setHorizontalAlignment(JTextField.RIGHT);
                int fontSize = textField.getFont().getSize();
                textField.setFont(new Font("Courier", Font.PLAIN, fontSize));
                bindingContext.bind(textField, model.getDescriptor().getName());
                editorComponent = textField;
            } else if (Boolean.class.isAssignableFrom(model.getDescriptor().getType())) {
                final JCheckBox checkBox = new JCheckBox();
                bindingContext.bind(checkBox, model.getDescriptor().getName());
                editorComponent = checkBox;
            } else if (File.class.isAssignableFrom(model.getDescriptor().getType())) {
                final JTextField textField = new JTextField();
                final Binding binding = bindingContext.bind(textField, model.getDescriptor().getName());
                JPanel subPanel = new JPanel(new BorderLayout(2, 2));
                subPanel.add(textField, BorderLayout.CENTER);
                JButton etcButton = new JButton("...");
                etcButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser fileChooser = new JFileChooser();
                        int i = fileChooser.showDialog(panel, "Select");
                        if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                            binding.setPropertyValue(fileChooser.getSelectedFile());
                        }
                    }
                });
                subPanel.add(etcButton, BorderLayout.EAST);
                editorComponent = subPanel;
            } else {
                if (model.getDescriptor().getValueSet() != null) {
                    JComboBox comboBox = new JComboBox();
                    bindingContext.bind(comboBox, model.getDescriptor().getName());
                    editorComponent = comboBox;
                } else {
                    JTextField textField = new JTextField();
                    bindingContext.bind(textField, model.getDescriptor().getName());
                    editorComponent = textField;
                }
            }
            editorComponent.setName(model.getDescriptor().getName());
            editorComponent.setToolTipText(model.getDescriptor().getDescription());
            JLabel label = new JLabel(getDisplayName(model) + ": ");
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            panel.add(label, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(editorComponent, gbc);
            gbc.gridy++;
        }
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);
        return panel;
    }

    private static String getDisplayName(ValueModel model) {
        Object label = model.getDescriptor().getDisplayName();
        if (label != null) {
            return label.toString();
        }
        String name = model.getDescriptor().getName().replace("_", " ");
        return createDisplayName(name);
    }

    static String createDisplayName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(ch));
            } else if (i > 0 && i < name.length() - 1
                    && Character.isUpperCase(ch) &&
                    Character.isLowerCase(name.charAt(i + 1))) {
                sb.append(' ');
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

}
