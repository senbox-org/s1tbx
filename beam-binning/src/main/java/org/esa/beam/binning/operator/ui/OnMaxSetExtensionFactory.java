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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;

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
            return new OnMaxSetPropertyEditor();
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

    private class OnMaxSetPropertyEditor extends PropertyEditor {

        @Override
        public JComponent createEditorComponent(final PropertyDescriptor propertyDescriptor, final BindingContext bindingContext) {
            JLabel label = new JLabel("Raster names");
            label.setToolTipText("The raster names that are set when the chosen source raster has the maximum value");
            Product[] sourceProducts = model.getSourceProducts();
            String[] rasterNames = new String[0];
            if (sourceProducts.length != 0) {
                Product product = sourceProducts[0];
                rasterNames = getRasterNames(product);
            }
            final JList<String> list = new JList<>(rasterNames);

            list.addListSelectionListener(new ListSelectionListener() {
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
            });

            JPanel panel = new JPanel(new BorderLayout());

            panel.add(label, BorderLayout.WEST);
            panel.add(list, BorderLayout.WEST);

            return panel;
        }
    }
}
