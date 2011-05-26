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

package org.esa.beam.framework.ui.layer;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for editors allowing to modify a layer's configuration.
 *
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class AbstractLayerConfigurationEditor extends AbstractLayerEditor {

    private BindingContext bindingContext;

    /**
     * @return The binding context.
     */
    protected final BindingContext getBindingContext() {
        return bindingContext;
    }

    @Override
    public JComponent createControl() {
        // TODO - replace this code block with the following line (rq-20090528)
        // bindingContext = new BindingContext(layer.getConfiguration());
        bindingContext = new BindingContext();
        PropertySet propertySet = bindingContext.getPropertySet();
        propertySet.addPropertyChangeListener(new PropertyChangeHandler());
        addEditablePropertyDescriptors();
        // ODOT

        PropertyPane propertyPane = new PropertyPane(bindingContext);
        return propertyPane.createPanel();
    }

    @Override
    public void handleLayerContentChanged() {
        final Property[] properties = bindingContext.getPropertySet().getProperties();
        for (Property property : properties) {
            final PropertyDescriptor propertyDescriptor = property.getDescriptor();
            String propertyName = propertyDescriptor.getName();
            Binding binding = bindingContext.getBinding(propertyName);
            PropertySet configuration = getCurrentLayer().getConfiguration();
            if (configuration.getProperty(propertyName) != null) {
                final Object value = configuration.getProperty(propertyName).getValue();
                final Object oldValue = binding.getPropertyValue();
                if (oldValue != value && (oldValue == null || !oldValue.equals(value))) {
                    binding.setPropertyValue(value);
                }
            }
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void handleEditorAttached() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void handleEditorDetached() {
    }

    /**
     * Clients overide in order to subsequently call {@link #addPropertyDescriptor(com.bc.ceres.binding.PropertyDescriptor)}
     * for each property that shall be editable by this editor.
     */
    protected abstract void addEditablePropertyDescriptors();

    /**
     * Defines an editable property.
     *
     * @param propertyDescriptor The property's descriptor.
     */
    protected final void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        Map<String, Object> valueData = new HashMap<String, Object>();
        String propertyName = propertyDescriptor.getName();
        Object value = getCurrentLayer().getConfiguration().getValue(propertyName);
        if (value == null) {
            value = propertyDescriptor.getDefaultValue();
        }
        valueData.put(propertyName, value);
        PropertyAccessor accessor = new MapEntryAccessor(valueData, propertyName);
        Property model = new Property(propertyDescriptor, accessor);
        bindingContext.getPropertySet().addProperty(model);
    }

    private class PropertyChangeHandler implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (getCurrentLayer() != null) {
                try {
                    final Property property = getCurrentLayer().getConfiguration().getProperty(propertyName);
                    if (property != null) {
                        property.setValue(evt.getNewValue());
                    }
                } catch (ValidationException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        }
    }
}
