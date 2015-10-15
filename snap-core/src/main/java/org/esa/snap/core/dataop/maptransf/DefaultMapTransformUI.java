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
package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Reproject'
 */
@Deprecated
public class DefaultMapTransformUI implements MapTransformUI {

    private MapTransform _transform;
    private Parameter[] _parameters;
    private Component _uiComponent;

    public DefaultMapTransformUI(MapTransform transform) {
        Guardian.assertNotNull("transform", transform);
        Debug.trace("DefaultMapTransformUI.init");
        _transform = transform;
        _parameters = cloneParameterArray(transform.getDescriptor().getParameters());
        setParameterValues(transform.getParameterValues());
        traceParameterValues();
    }

    public MapTransform createTransform() {
        Debug.trace("DefaultMapTransformUI.createTransform");
        traceParameterValues();
        final MapTransform transform = _transform.getDescriptor().createTransform(getParameterValues());
        final double[] parameterValues = transform.getParameterValues();
        for (int i = 0; i < parameterValues.length; i++) {
            Debug.trace("  MapTransform.parameterValues[" + i + "] = " + parameterValues[i]);
        }
        return transform;
    }

    public boolean verifyUserInput() {
        Debug.trace("DefaultMapTransformUI.verifyUserInput");
        return true;
    }

    public void resetToDefaults() {
        Debug.trace("DefaultMapTransformUI.resetToDefaults");
        setParameterValues(_transform.getDescriptor().getParameterDefaultValues());
    }

    public Component getUIComponent() {
        if (_uiComponent == null) {
            _uiComponent = createUIComponent();
        }
        return _uiComponent;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private Component createUIComponent() {
        return createUIComponent(_parameters);
    }

    private double[] getParameterValues() {
        return getParameterValues(_parameters);
    }

    private void setParameterValues(final double[] parameterValues) {
        for (int i = 0; i < _parameters.length; i++) {
            Parameter parameter = _parameters[i];
            parameter.setValue(parameterValues[i], null);
        }
    }

    private void traceParameterValues() {
        Debug.trace("DefaultMapTransformUI.traceParameterValues:");
        for (int i = 0; i < _parameters.length; i++) {
            Debug.trace("  DefaultMapTransformUI.parameters[" + i + "] = " + _parameters[i].getValue());
        }
    }

    // @todo 3 nf/nf - make public in a utility class
    /**
     * Creates a default UI component which can be used to edit the given parameters.
     */
    private static Component createUIComponent(final Parameter[] parameters) {
        final JPanel dialogPane = new JPanel(new GridBagLayout());
        dialogPane.setBorder(new EmptyBorder(2, 2, 2, 2));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 3, 0, 3);

        Parameter parameter;
        for (int i = 0; i < parameters.length; i++) {
            parameter = parameters[i];
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            gbc.insets.top = 0;
            dialogPane.add(parameter.getEditor().getLabelComponent(), gbc);
            dialogPane.add(parameter.getEditor().getComponent(), gbc);
            dialogPane.add(parameter.getEditor().getPhysUnitLabelComponent(), gbc);
        }
        return dialogPane;
    }

    // @todo 3 nf/nf - make public in a utility class
    /**
     * Creates a default UI component which can be used to edit the given parameters.
     */
    private static double[] getParameterValues(final Parameter[] parameters) {
        Guardian.assertNotNull("parameters", parameters);
        double[] parameterValues = new double[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            if (!(parameter.getValue() instanceof Number)) {
                throw new IllegalArgumentException("parameter value is not a number: " + i);
            }
            parameterValues[i] = ((Number) parameter.getValue()).doubleValue();
        }
        return parameterValues;
    }

    // @todo 3 nf/nf - make public in a utility class
    /**
     * Creates a default UI component which can be used to edit the given parameters.
     */
    private static Parameter[] cloneParameterArray(final Parameter[] parameters) {
        Guardian.assertNotNull("parameters", parameters);
        Parameter[] clones = new Parameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            clones[i] = new Parameter(parameter.getName(),
                                      parameter.getValue(),
                                      parameter.getProperties());
        }
        return clones;
    }
}
