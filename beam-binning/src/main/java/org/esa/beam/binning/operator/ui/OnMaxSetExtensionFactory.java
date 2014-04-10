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

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;

/**
* @author thomas
*/
class OnMaxSetExtensionFactory implements ExtensionFactory {

    private final BinningFormModel model;

    OnMaxSetExtensionFactory(BinningFormModel model) {
        this.model = model;
    }

    @Override
    public PropertyEditor getExtension(Object object, Class<?> extensionType) {
        if (extensionType.equals(getExtensionTypes()[0])) {
            return new OnMaxSetPropertyEditor(model);
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

    private static class OnMaxSetPropertyEditor extends PropertyEditor {

        private BinningFormModel model;

        private OnMaxSetPropertyEditor(BinningFormModel model) {
            this.model = model;
        }

        @Override
        public JComponent createEditorComponent(final PropertyDescriptor propertyDescriptor, final BindingContext bindingContext) {
            final String errorMessage = "no rasters available";
            JLabel label = new JLabel("Raster names");
            label.setToolTipText("The raster names that are set when the chosen source raster has the maximum value");
            Product[] sourceProducts = model.getSourceProducts();
            String[] rasterNames = new String[] {errorMessage};
            final JList<String> list = new JList<>();
            list.setVisibleRowCount(8);
            list.setFixedCellHeight(15);
            list.setFixedCellWidth(100);
            if (sourceProducts.length != 0) {
                Product product = sourceProducts[0];
                rasterNames = getRasterNames(product);
            } else {
                setUpEmptyList(list);
            }
            list.setListData(rasterNames);
            list.addListSelectionListener(createListSelectionListener(propertyDescriptor, bindingContext, list));

            TableLayout layout = new TableLayout(2);
            layout.setColumnWeightX(1, 1.0);
            layout.setTablePadding(10, 5);
            layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);

            JPanel panel = new JPanel(layout);
            panel.add(label);
            panel.add(new JScrollPane(list));

            return panel;
        }
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
                    Property aggregatorPropertiesProperty = bindingContext.getPropertySet().getProperty(propertyDescriptor.getName());
                    PropertyContainer aggregatorProperties = aggregatorPropertiesProperty.getValue();
                    aggregatorProperties.getProperty("varNames").setValue(values);
                } catch (ValidationException e1) {
                    // todo
                    e1.printStackTrace();
                }
            }
        };
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
    }

}
