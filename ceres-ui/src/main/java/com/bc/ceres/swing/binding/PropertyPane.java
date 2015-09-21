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

package com.bc.ceres.swing.binding;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import static com.bc.ceres.swing.TableLayout.*;

/**
 * A utility class used to create a {@link JPanel} containing default Swing components and their corresponding bindings for the
 * {@link com.bc.ceres.binding.PropertyContainer} given by the {@link com.bc.ceres.swing.binding.BindingContext}.
 * <p>If the {@code displayName} property of the binding's {@link com.bc.ceres.binding.PropertySet PropertySet}
 * is set, it will be used as label, otherwise a label is derived from the {@code name} property.
 * <p>Properties, whose attribute "enabled" is set to {@code false}, will be shown in disabled state.
 * Properties, whose attribute "visible" is set to {@code false}, will not be shown at all.
 */
public class PropertyPane {

    private final BindingContext bindingContext;

    public PropertyPane(PropertySet propertySet) {
        this(new BindingContext(propertySet));
    }

    public PropertyPane(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {
        PropertySet propertyContainer = bindingContext.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        boolean displayUnitColumn = wantDisplayUnitColumn(properties);
        TableLayout layout = new TableLayout(displayUnitColumn ? 3 : 2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;
        final PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            if (isInvisible(descriptor)) {
                continue;
            }
            PropertyEditor propertyEditor = registry.findPropertyEditor(descriptor);
            JComponent[] components = propertyEditor.createComponents(descriptor, bindingContext);
            if (components.length == 2) {
                layout.setCellWeightX(rowIndex, 0, 0.0);
                panel.add(components[1], cell(rowIndex, 0));
                layout.setCellWeightX(rowIndex, 1, 1.0);
                if(components[0] instanceof JScrollPane) {
                    layout.setRowWeightY(rowIndex, 1.0);
                    layout.setRowFill(rowIndex, TableLayout.Fill.BOTH);
                }
                panel.add(components[0], cell(rowIndex, 1));
            } else {
                layout.setCellColspan(rowIndex, 0, 2);
                layout.setCellWeightX(rowIndex, 0, 1.0);
                panel.add(components[0], cell(rowIndex, 0));
            }
            if (displayUnitColumn) {
                final JLabel label = new JLabel("");
                if (descriptor.getUnit() != null) {
                    label.setText(descriptor.getUnit());
                }
                layout.setCellWeightX(rowIndex, 2, 0.0);
                panel.add(label, cell(rowIndex, 2));
            }
            rowIndex++;
        }
        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        layout.setCellWeightY(rowIndex, 0, 0.5);
        panel.add(new JPanel());
        return panel;
    }

    private boolean isInvisible(PropertyDescriptor descriptor) {
        return Boolean.FALSE.equals(descriptor.getAttribute("visible")) || descriptor.isDeprecated();
    }

    private boolean wantDisplayUnitColumn(Property[] models) {
        boolean showUnitColumn = false;
        for (Property model : models) {
            PropertyDescriptor descriptor = model.getDescriptor();
            if (isInvisible(descriptor)) {
                continue;
            }
            String unit = descriptor.getUnit();
            if (!(unit == null || unit.length() == 0)) {
                showUnitColumn = true;
                break;
            }
        }
        return showUnitColumn;
    }
}
