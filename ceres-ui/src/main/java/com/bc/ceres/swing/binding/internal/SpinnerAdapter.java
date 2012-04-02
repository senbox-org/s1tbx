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
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A binding for a {@link JSpinner} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class SpinnerAdapter extends ComponentAdapter implements ChangeListener {

    final JSpinner spinner;

    public SpinnerAdapter(JSpinner spinner) {
        this.spinner = spinner;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{spinner};
    }

    @Override
    public void bindComponents() {
        updateSpinnerModel();
        spinner.addChangeListener(this);
    }

    @Override
    public void unbindComponents() {
        spinner.removeChangeListener(this);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        spinner.setValue(value);
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        getBinding().setPropertyValue(spinner.getValue());
    }

    private void updateSpinnerModel() {
        PropertyDescriptor propertyDescriptor = getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
        ValueRange valueRange = propertyDescriptor.getValueRange();
        if (valueRange != null) {
            Object propertyValue = getBinding().getPropertyValue();
            if (propertyValue == null) {
                propertyValue = propertyDescriptor.getDefaultValue();
            }
            final Object stepSizeValue = propertyDescriptor.getAttribute("stepSize");
            if (propertyValue instanceof Float || propertyValue instanceof Double) {
                double value = ((Number) propertyValue).doubleValue();
                double min = valueRange.getMin();
                double max = valueRange.getMax();
                double stepSize = (stepSizeValue instanceof Number ? (Number) stepSizeValue : 1.0).doubleValue();
                spinner.setModel(new SpinnerNumberModel(value, min, max, stepSize));
            } else {
                int value = ((Number) propertyValue).intValue();
                int min = (int) valueRange.getMin();
                int max = (int) valueRange.getMax();
                int stepSize = (stepSizeValue instanceof Number ? (Number) stepSizeValue : 1).intValue();
                spinner.setModel(new SpinnerNumberModel(value, min, max, stepSize));
            }
        } else if (propertyDescriptor.getValueSet() != null) {
            spinner.setModel(new SpinnerListModel(propertyDescriptor.getValueSet().getItems()));
        }
    }
}
