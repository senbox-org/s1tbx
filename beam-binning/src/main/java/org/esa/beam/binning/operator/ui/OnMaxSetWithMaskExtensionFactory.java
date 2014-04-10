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
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* @author thomas
*/
class OnMaxSetWithMaskExtensionFactory implements ExtensionFactory {

    private final BinningFormModel model;

    OnMaxSetWithMaskExtensionFactory(BinningFormModel model) {
        this.model = model;
    }

    @Override
    public PropertyEditor getExtension(Object object, Class<?> extensionType) {
        if (extensionType.equals(getExtensionTypes()[0])) {
            return new OnMaxSetWithMaskPropertyEditor(model);
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class[]{PropertyEditor.class};
    }

    private static String[] getRasterNames(Product product) {
        String[] bandNames = product.getBandNames();
        String[] tiePointGridNames = product.getTiePointGridNames();
        String[] maskNames = product.getMaskGroup().getNodeNames();
        String[] rasterNames = new String[bandNames.length + tiePointGridNames.length + maskNames.length];
        System.arraycopy(bandNames, 0, rasterNames, 0, bandNames.length);
        System.arraycopy(tiePointGridNames, 0, rasterNames, bandNames.length, tiePointGridNames.length);
        System.arraycopy(maskNames, 0, rasterNames, bandNames.length + tiePointGridNames.length, maskNames.length);
        return rasterNames;
    }

    private static class OnMaxSetWithMaskPropertyEditor extends PropertyEditor {

        private BinningFormModel model;

        private OnMaxSetWithMaskPropertyEditor(BinningFormModel model) {
            this.model = model;
        }

        @Override
        public JComponent createEditorComponent(final PropertyDescriptor propertyDescriptor, final BindingContext bindingContext) {
            Property aggregatorPropertiesProperty = bindingContext.getPropertySet().getProperty(propertyDescriptor.getName());
            PropertyContainer aggregatorProperties = aggregatorPropertiesProperty.getValue();
            Property varName = bindingContext.getPropertySet().getProperty("varName");
            aggregatorProperties.setValue("onMaxName", varName.getValue());

            JLabel maskLabel = new JLabel("Mask name");
            maskLabel.setToolTipText("The mask name");

            JLabel rasterLabel = new JLabel("Raster names");
            rasterLabel.setToolTipText("The raster names that are set when the chosen source has the maximum value");

            Product[] sourceProducts = model.getSourceProducts();
            final JComboBox<String> maskDropDown = new JComboBox<>();
            final JList<String> rasterList = new JList<>();
            rasterList.setVisibleRowCount(8);
            rasterList.setFixedCellHeight(15);
            rasterList.setFixedCellWidth(100);
            if (sourceProducts.length != 0) {
                Product product = sourceProducts[0];
                String[] rasterNames = getRasterNames(product);
                rasterList.setListData(rasterNames);
                selectOldRasters(propertyDescriptor, bindingContext, rasterList, rasterNames);
            } else {
                setUpEmptyList(rasterList);
            }
            if (sourceProducts.length != 0 && sourceProducts[0].getMaskGroup().getNodeCount() > 0) {
                maskDropDown.setModel(new DefaultComboBoxModel<>(sourceProducts[0].getMaskGroup().getNodeNames()));
                selectOldMask(propertyDescriptor, bindingContext, maskDropDown);
            } else {
                setUpEmptyDropDown(maskDropDown);
            }

            rasterList.addListSelectionListener(createListSelectionListener(propertyDescriptor, bindingContext, rasterList));
            maskDropDown.addActionListener(createDropDownListener(propertyDescriptor, bindingContext, maskDropDown));

            TableLayout layout = new TableLayout(2);
            layout.setColumnWeightX(1, 1.0);
            layout.setTablePadding(10, 5);
            layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);

            JPanel panel = new JPanel(layout);
            panel.add(maskLabel);
            panel.add(maskDropDown);
            panel.add(rasterLabel);
            panel.add(new JScrollPane(rasterList));

            return panel;
        }
    }

    private static void selectOldMask(PropertyDescriptor propertyDescriptor, BindingContext bindingContext, JComboBox<String> maskDropDown) {
        Property maskName = getProperty("maskName", propertyDescriptor, bindingContext);
        maskDropDown.setSelectedItem(maskName.getValue());
    }

    private static ActionListener createDropDownListener(final PropertyDescriptor propertyDescriptor, final BindingContext bindingContext, final JComboBox<String> maskDropDown) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Property maskName = getProperty("maskName", propertyDescriptor, bindingContext);
                    maskName.setValue(maskDropDown.getSelectedItem());
                } catch (ValidationException e1) {
                    throw new IllegalStateException(e1);
                }
            }
        };
    }

    private static void selectOldRasters(PropertyDescriptor propertyDescriptor, BindingContext bindingContext, JList<String> list, String[] rasterNames) {
        String[] varNames = getSetNames(propertyDescriptor, bindingContext);
        for (int i = 0; i < rasterNames.length; i++) {
            for (final String varName : varNames) {
                if (rasterNames[i].equals(varName)) {
                    list.addSelectionInterval(i, i);
                }
            }
        }
    }

    private static String[] getSetNames(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        Property property = getProperty("setNames", propertyDescriptor, bindingContext);
        String[] varNames = property.getValue();
        if (varNames == null) {
            varNames = new String[0];
        }
        return varNames;
    }

    private static ListSelectionListener createListSelectionListener(final PropertyDescriptor propertyDescriptor, final BindingContext bindingContext, final JList<String> list) {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int[] selectedIndices = list.getSelectedIndices();
                String[] values = new String[selectedIndices.length];
                for (int i = 0; i < selectedIndices.length; i++) {
                    values[i] = list.getModel().getElementAt(selectedIndices[i]);
                }
                try {
                    Property varNames = getProperty("setNames", propertyDescriptor, bindingContext);
                    varNames.setValue(values);
                } catch (ValidationException e1) {
                    throw new IllegalStateException(e1);
                }
            }
        };
    }

    private static Property getProperty(String propertyName, PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        Property aggregatorPropertiesProperty = bindingContext.getPropertySet().getProperty(propertyDescriptor.getName());
        PropertyContainer aggregatorProperties = aggregatorPropertiesProperty.getValue();
        return aggregatorProperties.getProperty(propertyName);
    }

    private static void setUpEmptyList(JList<String> list) {
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> _list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Component component = super.getListCellRendererComponent(_list, value, index, isSelected, cellHasFocus);
                component.setEnabled(false);
                return component;
            }
        });
        String[] rasterNames = new String[] {"no rasters available"};
        list.setListData(rasterNames);
    }

    private static void setUpEmptyDropDown(JComboBox<String> comboBox) {
        comboBox.setEnabled(false);
        comboBox.setModel(new DefaultComboBoxModel<>(new String[]{"no masks available"}));
    }

}
