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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PixelExtractionDialogForm {

    private static final String[] PRODUCT_TYPES = new String[]{
            "MER_FR__1P",
            "MER_RR__1P",
            "MER_FRS_1P",
            "MER_FSG_1P",
            "MER_FRG_1P",
            "MER_FR__2P",
            "MER_RR__2P",
            "MER_FRS_2P",
            "ATS_TOA_1P",
            "ATS_NR__2P"
    };

    private JPanel panel;

    public PixelExtractionDialogForm(PropertyContainer container) {

        final TableLayout tableLayout = createLayout();

        panel = new JPanel();
        panel.setLayout(tableLayout);
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(new JLabel("Product Type"));
        panel.add(createProductTypeEditor(bindingContext));
        panel.add(tableLayout.createHorizontalSpacer());

        panel.add(new JLabel("Input paths"));
        panel.add(createInputFolderChooser(bindingContext));
        panel.add(createInputButton(bindingContext));

        panel.add(new JLabel("Square size"));
        panel.add(createSquareSizeEditor(container, bindingContext));
        panel.add(tableLayout.createHorizontalSpacer());
    }

    private JTextField createInputFolderChooser(BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        bindingContext.bind("inputPaths", textField);
        return textField;
    }

    private JButton createInputButton(BindingContext bindingContext) {
        final Binding binding = bindingContext.getBinding("inputPaths");
        final JButton etcButton = new JButton("+");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                int i = fileChooser.showDialog(panel, "Select");
                if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                    binding.setPropertyValue(fileChooser.getSelectedFile());
                }
            }
        });
        return etcButton;
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setTableWeightY(0.0);
        return tableLayout;
    }

    private JComponent createProductTypeEditor(BindingContext binding) {
        final JComboBox productTypesBox = new JComboBox(PRODUCT_TYPES);
        productTypesBox.setEditable(true);
        binding.bind("productType", productTypesBox);
        productTypesBox.setSelectedIndex(0);
        return productTypesBox;
    }

    private JComponent createSquareSizeEditor(PropertyContainer container, BindingContext binding) {
        final Property squareSizeProperty = container.getProperty("squareSize");
        final Number defaultValue = (Number) squareSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        binding.bind("squareSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

}
