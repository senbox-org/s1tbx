/*
 * $Id: DefaultMapTransformUI.java,v 1.2 2006/10/09 13:11:55 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.maptransf;

import java.awt.*;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

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
