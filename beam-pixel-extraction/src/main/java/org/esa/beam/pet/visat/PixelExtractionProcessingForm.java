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
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PixelExtractionProcessingForm {

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

    public PixelExtractionProcessingForm(AppContext appContext, PropertyContainer container) {

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellWeightY(1, 1, 1.0);
        tableLayout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        tableLayout.setCellWeightY(2, 1, 1.0);
        tableLayout.setCellFill(2, 1, TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        panel = new JPanel(tableLayout);
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(new JLabel("Product type:"));
        panel.add(createProductTypeEditor(bindingContext));
        panel.add(new JLabel());

        panel.add(new JLabel("Rasters:"));
        final JScrollPane rasterScrollPane = new JScrollPane(createRasterEditor());
        setScrollbarPolicy(rasterScrollPane);
        panel.add(rasterScrollPane);
        panel.add(new JLabel());

        panel.add(new JLabel("Coordinates:"));
        final JScrollPane coordScrollPane = new JScrollPane(createCoordinatesEditor());
        setScrollbarPolicy(coordScrollPane);
        panel.add(coordScrollPane);
        panel.add(new JLabel());

        panel.add(new JLabel("Pin file:"));
        final JComponent[] pinFileComponents = createPinFileComponents(bindingContext);
        panel.add(pinFileComponents[0]);
        panel.add(pinFileComponents[1]);

        panel.add(new JLabel("Square size:"));
        panel.add(createSquareSizeEditor(container, bindingContext));
        panel.add(new JLabel());

        panel.add(tableLayout.createVerticalSpacer());
    }

    private void setScrollbarPolicy(JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    private JComponent[] createPinFileComponents(BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind("coordinatesFile", adapter);
        final JButton ellipsesButton = new JButton("...");
        ellipsesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                int i = fileChooser.showDialog(panel, "Select");
                if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                    binding.setPropertyValue(fileChooser.getSelectedFile());
                }
            }
        });
        return new JComponent[]{textField, ellipsesButton};
    }

    private JComponent createRasterEditor() {
        JList rasterList = new JList(new String[]{"radiance_1", "radiance_2"});
        rasterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return rasterList;
    }

    private JComponent createCoordinatesEditor() {
        JList rasterList = new JList(new String[]{"40.5,12.67", "35.8,25.7"});
        rasterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return rasterList;
    }

    private JComponent createProductTypeEditor(BindingContext bindingContext) {
        final JComboBox productTypesBox = new JComboBox(PRODUCT_TYPES);
        productTypesBox.setEditable(true);
        bindingContext.bind("productType", productTypesBox);
        productTypesBox.setSelectedIndex(0);
        return productTypesBox;
    }

    private JComponent createSquareSizeEditor(PropertyContainer container, BindingContext bindingContext) {
        final Property squareSizeProperty = container.getProperty("squareSize");
        final Number defaultValue = (Number) squareSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        bindingContext.bind("squareSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

}
