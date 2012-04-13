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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
        bindingContext = new BindingContext();
        PropertySet propertySet = bindingContext.getPropertySet();
        propertySet.addPropertyChangeListener(new PropertyChangeHandler());
        addEditablePropertyDescriptors();
        PropertyPane propertyPane = new PropertyPane(bindingContext);
        return propertyPane.createPanel();
    }

    @Override
    public void handleLayerContentChanged() {
        final Property[] editorProperties = bindingContext.getPropertySet().getProperties();
        for (Property editorProperty : editorProperties) {
            final String propertyName = editorProperty.getDescriptor().getName();
            final Property layerProperty = getCurrentLayer().getConfiguration().getProperty(propertyName);
            if (layerProperty != null) {
                final Binding binding = bindingContext.getBinding(propertyName);
                final Object layerValue = layerProperty.getValue();
                final Object editorValue = binding.getPropertyValue();
                if (editorValue != layerValue && (editorValue == null || !editorValue.equals(layerValue))) {
                    binding.setPropertyValue(layerValue);
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
     * Clients override in order to subsequently call {@link #addPropertyDescriptor(com.bc.ceres.binding.PropertyDescriptor)}
     * for each property that shall be editable by this editor.
     */
    protected abstract void addEditablePropertyDescriptors();

    /**
     * Defines an editable property.
     *
     * @param propertyDescriptor The property's descriptor.
     */
    protected final void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        String propertyName = propertyDescriptor.getName();
        Object value = getCurrentLayer().getConfiguration().getValue(propertyName);
        if (value == null) {
            value = propertyDescriptor.getDefaultValue();
        }
        Property editorProperty = new Property(propertyDescriptor, new DefaultPropertyAccessor(value));
        bindingContext.getPropertySet().addProperty(editorProperty);
    }

    private class PropertyChangeHandler implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (getCurrentLayer() != null) {
                try {
                    final Property layerProperty = getCurrentLayer().getConfiguration().getProperty(propertyName);
                    if (layerProperty != null) {
                        layerProperty.setValue(evt.getNewValue());
                    }
                } catch (ValidationException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        }
    }
}
