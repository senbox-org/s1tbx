package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.internal.*;
import com.bc.ceres.core.Assert;

import javax.swing.*;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
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
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final JFormattedTextField textField, final String propertyName) {
        configureComponent(textField, propertyName);
        Binding binding = new FormattedTextFieldBinding(this, textField, propertyName);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final JCheckBox checkBox, final String propertyName) {
        configureComponent(checkBox, propertyName);
        Binding binding = new CheckBoxBinding(this, propertyName, checkBox);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(JRadioButton radioButton, String propertyName) {
        configureComponent(radioButton, propertyName);
        Binding binding = new RadioButtonBinding(this, propertyName, radioButton);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final JList list, final String propertyName, final boolean selectionIsValue) {
        if (selectionIsValue) {
            configureComponent(list, propertyName);
            Binding binding = new ListSelectionBinding(this, list, propertyName);
            binding.adjustComponents();
            return binding;
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public Binding bind(final JSpinner spinner, final String propertyName) {
        configureComponent(spinner, propertyName);
        Binding binding = new SpinnerBinding(this, propertyName, spinner);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final JComboBox comboBox, final String propertyName) {
        configureComponent(comboBox, propertyName);
        Binding binding = new ComboBoxBinding(this, propertyName, comboBox);
        binding.adjustComponents();
        return binding;
    }

    public Binding bind(final ButtonGroup buttonGroup, final String propertyName) {
        return bind(buttonGroup, propertyName,
                    ButtonGroupBinding.createButtonToValueMap(buttonGroup, getValueContainer(), propertyName));
    }

    public Binding bind(final ButtonGroup buttonGroup, final String propertyName,
                        final Map<AbstractButton, Object> propertyValues) {
        Binding binding = new ButtonGroupBinding(this, buttonGroup, propertyName, propertyValues);
        binding.adjustComponents();
        return binding;
    }

    public void enable(final JComponent component, final String propertyName, final Object propertyCondition) {
        bindEnabling(component, propertyName, propertyCondition, true);
    }

    public void disable(final JComponent component, final String propertyName, final Object propertyCondition) {
        bindEnabling(component, propertyName, propertyCondition, false);
    }

    /**
     * Delegates the call to the error handler.
     * @param exception The error.
     * @param component The Swing component in which the error occured.
     * @see #getErrorHandler()
     * @see #setErrorHandler(ErrorHandler)
     */
    public void handleError(Exception exception, JComponent component) {
        errorHandler.handleError(exception, component);
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

        void handleError(Exception exception, JComponent component);
    }

    public static class DefaultErrorHandler implements BindingContext.ErrorHandler {

        public void handleError(Exception exception, JComponent component) {
            Window window = component != null ? SwingUtilities.windowForComponent(component) : null;
            if (exception instanceof ValidationException) {
                JOptionPane.showMessageDialog(window, exception.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } else {
                String message = MessageFormat.format("An internal error occured:\nType: {0}\nMessage: {1}\n", exception.getClass(), exception.getMessage());
                JOptionPane.showMessageDialog(window, message, "Internal Error", JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
            if (component != null) {
                component.requestFocus();
            }
        }
    }


}
