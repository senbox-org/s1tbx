package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.internal.*;
import com.bc.ceres.core.Assert;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * A context used to bind Swing components to properties in a value container.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.1
 */
public class BindingContext {

    private final ValueContainer valueContainer;
    private BindingContext.ErrorHandler errorHandler;

    public BindingContext(ValueContainer valueContainer) {
        this(valueContainer, new BindingContext.DefaultErrorHandler());
    }

    public BindingContext(ValueContainer valueContainer, BindingContext.ErrorHandler errorHandler) {
        this.valueContainer = valueContainer;
        this.errorHandler = errorHandler;
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Binding bind(final JTextField textField, final String propertyName) {
        configureComponent(textField, propertyName);
        Binding binding = new TextFieldBinding(this, textField, propertyName);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(final JFormattedTextField textField, final String propertyName) {
        configureComponent(textField, propertyName);
        Binding binding = new FormattedTextFieldBinding(this, textField, propertyName);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(final JCheckBox checkBox, final String propertyName) {
        configureComponent(checkBox, propertyName);
        Binding binding = new CheckBoxBinding(this, propertyName, checkBox);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(JRadioButton radioButton, String propertyName) {
        configureComponent(radioButton, propertyName);
        Binding binding = new RadioButtonBinding(this, propertyName, radioButton);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(final JList list, final String propertyName, final boolean selectionIsValue) {
        if (selectionIsValue) {
            configureComponent(list, propertyName);
            Binding binding = new ListSelectionBinding(this, list, propertyName);
            binding.adjustComponent();
            return binding;
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public Binding bind(final JSpinner spinner, final String propertyName) {
        configureComponent(spinner, propertyName);
        Binding binding = new SpinnerBinding(this, propertyName, spinner);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(final JComboBox comboBox, final String propertyName) {
        configureComponent(comboBox, propertyName);
        Binding binding = new ComboBoxBinding(this, propertyName, comboBox);
        binding.adjustComponent();
        return binding;
    }

    public Binding bind(final ButtonGroup buttonGroup, final String propertyName) {
        return bind(buttonGroup, propertyName,
                    ButtonGroupBinding.createButtonToValueMap(buttonGroup, getValueContainer(), propertyName));
    }

    public Binding bind(final ButtonGroup buttonGroup, final String propertyName,
                        final Map<AbstractButton, Object> propertyValues) {
        Binding binding = new ButtonGroupBinding(this, buttonGroup, propertyName, propertyValues);
        binding.adjustComponent();
        return binding;
    }

    public void enable(final JComponent component, final String propertyName, final Object propertyCondition) {
        bindEnabling(component, propertyName, propertyCondition, true);
    }

    public void disable(final JComponent component, final String propertyName, final Object propertyCondition) {
        bindEnabling(component, propertyName, propertyCondition, false);
    }

    public void handleError(JComponent component, Exception e) {
        errorHandler.handleError(component, e);
    }

    private void configureComponent(JComponent component, String propertyName) {
        Assert.notNull(component, "component");
        Assert.notNull(propertyName, "propertyName");
        final ValueModel valueModel = valueContainer.getModel(propertyName);
        Assert.argument(valueModel != null, "Undefined property '" + propertyName + "'");
        if (component.getName() == null) {
            component.setName(propertyName);
        }
        if (valueModel != null) {
            if (component.getToolTipText() == null) {
                StringBuilder toolTipText = new StringBuilder();
                final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
                if (valueDescriptor.getDescription() != null) {
                    toolTipText.append(valueDescriptor.getDescription());
                }
                if (valueDescriptor.getUnit() != null && !valueDescriptor.getUnit().isEmpty()) {
                    toolTipText.append(" [");
                    toolTipText.append(valueDescriptor.getUnit());
                    toolTipText.append("]");
                }
                if (toolTipText.length() > 0) {
                    component.setToolTipText(toolTipText.toString());
                }
            }
        }
    }


    private void bindEnabling(final JComponent component,
                              final String propertyName,
                              final Object propertyCondition,
                              final boolean componentEnabled) {
        valueContainer.addPropertyChangeListener(propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setEnabled(component, propertyName, propertyCondition, componentEnabled);
            }
        });
        setEnabled(component, propertyName, propertyCondition, componentEnabled);
    }

    private void setEnabled(JComponent component,
                            String propertyName,
                            Object propertyCondition,
                            boolean componentEnabled) {
        Object propertyValue = valueContainer.getValue(propertyName);
        boolean conditionIsTrue = propertyValue == propertyCondition
                || (propertyValue != null && propertyValue.equals(propertyCondition));
        component.setEnabled(conditionIsTrue ? componentEnabled : !componentEnabled);
    }

    public interface ErrorHandler {

        void handleError(JComponent component, Exception e);
    }

    public static class DefaultErrorHandler implements BindingContext.ErrorHandler {

        public void handleError(JComponent component, Exception e) {
            JOptionPane.showMessageDialog(component, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            component.requestFocus();
        }
    }


}
