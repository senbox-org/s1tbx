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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

/**
 * An editor for file names using a file chooser dialog.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FileEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return File.class.isAssignableFrom(propertyDescriptor.getType())
               && !Boolean.TRUE.equals(propertyDescriptor.getAttribute("directory"));
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel editorPanel = new JPanel(new BorderLayout(2, 2));
        editorPanel.add(textField, BorderLayout.CENTER);
        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            File currentFile = (File) binding.getPropertyValue();
            if (currentFile != null) {
                fileChooser.setSelectedFile(currentFile);
            } else {
                File selectedFile = (File) propertyDescriptor.getDefaultValue();
                fileChooser.setSelectedFile(selectedFile);
            }
            int i = fileChooser.showDialog(editorPanel, "Select");
            if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                binding.setPropertyValue(fileChooser.getSelectedFile());
            }
        });
        editorPanel.add(etcButton, BorderLayout.EAST);
        return editorPanel;
    }

}
