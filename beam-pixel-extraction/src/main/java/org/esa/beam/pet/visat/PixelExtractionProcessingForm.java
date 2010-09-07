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
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private AppContext appContext;

    public PixelExtractionProcessingForm(AppContext appContext, PropertyContainer container) {
        this.appContext = appContext;

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
        final JComponent[] rasterComponents = createRasterComponents(container);
        panel.add(rasterComponents[0]);
        panel.add(rasterComponents[1]);

        panel.add(new JLabel("Coordinates:"));
        final JComponent[] coordinatesComponents = createCoordinatesComponents(container);
        panel.add(coordinatesComponents[0]);
        panel.add(coordinatesComponents[1]);

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

    private JComponent[] createRasterComponents(PropertyContainer container) {

        final GenericListModel<String> listModel = new GenericListModel<String>(container.getProperty("rasters"));
        final JList rasterList = new JList(listModel);
        rasterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane rasterScrollPane = new JScrollPane(rasterList);
        setScrollbarPolicy(rasterScrollPane);

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    listModel.addElement(JOptionPane.showInputDialog("Enter raster name"));
                } catch (ValidationException ve) {
                    appContext.handleError("Invalid raster name", ve);
                }
            }
        });
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object[] objects = rasterList.getSelectedValues();
                final String[] strings = Arrays.copyOf(objects, objects.length, String[].class);
                listModel.removeElements(strings);
            }
        });
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        return new JComponent[]{rasterScrollPane, buttonPanel};
    }

    private JComponent[] createCoordinatesComponents(PropertyContainer container) {
        final GenericListModel<GeoPos> listModel = new GenericListModel<GeoPos>(container.getProperty("coordinates"));
        final JList coordinateList = new JList(listModel);
        coordinateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane rasterScrollPane = new JScrollPane(coordinateList);
        setScrollbarPolicy(rasterScrollPane);

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String result = JOptionPane.showInputDialog("Specify geo position", "00.0000; 00.0000");
                String[] positions = result.split(";");
                Float x = Float.parseFloat(positions[0].trim());
                Float y = Float.parseFloat(positions[1].trim());
                try {
                    listModel.addElement(new GeoPos(x, y));
                } catch (ValidationException ignored) {
                }
            }
        });
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.removeElements((GeoPos[]) coordinateList.getSelectedValues());
            }
        });
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        return new JComponent[]{rasterScrollPane, buttonPanel};
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
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Object value = spinner.getValue();
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue % 2 == 0) {
                        spinner.setValue(intValue + 1);
                    }
                }
            }
        });
        bindingContext.bind("squareSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

    private static class GenericListModel<T> extends AbstractListModel {

        private List<T> elementList;
        private Property property;

        private GenericListModel(Property property) {
            this.property = property;
            elementList = new ArrayList<T>();
        }

        @Override
        public int getSize() {
            return elementList.size();
        }

        @Override
        public Object getElementAt(int index) {
            return elementList.get(index);
        }

        public void addElement(T element) throws ValidationException {
            if (!elementList.contains(element)) {
                if (elementList.add(element)) {
                    fireIntervalAdded(this, 0, getSize());
                    updateProperty();
                }
            }
        }

        public void removeElements(T... elements) {
            for (T elem : elements) {
                if (elementList.remove(elem)) {
                    fireIntervalRemoved(this, 0, getSize());
                    try {
                        updateProperty();
                    } catch (ValidationException ignored) {
                    }
                }
            }
        }

        @SuppressWarnings({"unchecked"})
        private void updateProperty() throws ValidationException {
            final T[] array = (T[]) Array.newInstance(property.getType().getComponentType(), elementList.size());
            property.setValue(elementList.toArray(array));
        }
    }
}
